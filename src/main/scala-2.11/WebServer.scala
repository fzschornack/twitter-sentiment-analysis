import actors.SentimentAnalysisActor
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.settings.ServerSettings
import akka.http.scaladsl.settings.ServerSettings.Timeouts
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives._
import com.typesafe.config.ConfigFactory
import models.{JsonSupport, QueryParameters, SentimentAnalysisResponse}
import org.apache.log4j.{Level, LogManager}
import org.apache.spark.{SparkConf, SparkContext}
import spray.json._

import scala.io.StdIn
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Runs a WebServer that works as a REST API.
  * Accepts a POST request containing QueryParameters.
  * Those QueryParameters are sent to an akka Actor that performs
  * a query on ElasticSearch and then execute a sentiment analysis
  * on the retrieved data using Apache Spark.
  *
  * Created by fzschornack on 10/08/2016.
  */
object WebServer extends JsonSupport {

  // define the server timeout
  val server_timeout = 20.minutes
  // number of actors used
  val parallelism_level = 6

  def main(args: Array[String]) {

    // Spark configurations
    val conf = new SparkConf().setAppName("TwitterSentimentAnalysis").setMaster("local[*]")
    val sc = new SparkContext(conf)
    LogManager.getRootLogger.setLevel(Level.WARN)

    // akka HTTP and Actor implicits
    implicit val system = ActorSystem("tweet-sentiment-analysis-server")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher


    // create and distribute the workload among many akka Actors using a router
    val sentimentAnalysisActor = system.actorOf(Props(new SentimentAnalysisActor(sc)).withRouter(RoundRobinPool(parallelism_level)))

    // define the API endpoints and logic (use cors to accept cross-origin requests)
    val route: Route = cors() {
        path("sentiment") {
            post {
              implicit val timeout: Timeout = server_timeout

              entity(as[QueryParameters]) { queryParameters =>
                val response: Future[SentimentAnalysisResponse] = (sentimentAnalysisActor ? queryParameters).mapTo[SentimentAnalysisResponse]

                onComplete(response) {
                  case Success(response) => complete(response.toJson)
                  case Failure(t) => complete((StatusCodes.BadRequest, "An error has occured: " + t.getMessage))
                }
              }
            }
        }
      }

    // set the server timeouts
    val serverTimeouts = new Timeouts {
      override def idleTimeout: Duration = server_timeout
      override def bindTimeout: FiniteDuration = server_timeout
      override def requestTimeout: Duration = server_timeout
    }
    val serverSettings = ServerSettings(system).withTimeouts(serverTimeouts)

    // starts the HTTP Server
    val bindingFuture = Http().bindAndHandle(route, "localhost", 9001, settings = serverSettings)

    println(s"Server online at http://localhost:9001/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }

}
