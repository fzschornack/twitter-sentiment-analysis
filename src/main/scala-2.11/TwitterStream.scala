import actors.SaveInESActor
import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.settings.ClientConnectionSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Framing, RunnableGraph, Sink, Source}
import akka.util.ByteString
import com.sksamuel.elastic4s.ElasticClient
import com.typesafe.config.ConfigFactory
import org.elasticsearch.common.settings.Settings
import utils.TwitterUtils

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Connects and consumes data from the Twitter Stream API.
  * The incoming tweets are sent to akka Actors to be stored in Elastic Search.
  *
  * Created by fzschornack on 11/08/2016.
  */
object TwitterStream {

  // define a default timeout
  val connection_timeout = 300.seconds
  // number of actors used
  val parallelism_level = 6

  /**
    * args[0] Twitter Stream API track word
    * @param args
    */
  def main(args: Array[String]) {

    val twitterTrackWord = args(0)

    // akka HTTP, Stream and Actor implicits
    implicit val system = ActorSystem("tweet-sentiment-analysis")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    // load application.conf
    val appConf = ConfigFactory.load()

    // set HTTP outgoing connection settings
    val connectionSettings = ClientConnectionSettings(system)
      .withIdleTimeout(connection_timeout)
      .withConnectingTimeout(connection_timeout)

    // create the connection (main Flow) to the Twitter Stream API
    val connectionFlow = Http().outgoingConnectionHttps("stream.twitter.com", settings = connectionSettings)

    // generate the twitter Oauth authorization header
    val authorizationHeader = TwitterUtils.getAuthorizationHeader(HttpMethods.GET.value, "https://stream.twitter.com/1.1/statuses/filter.json", s"track=$twitterTrackWord")

    // create the request to be sent to the Twitter Stream API
    val request: HttpRequest = HttpRequest(
      HttpMethods.GET,
      uri = s"/1.1/statuses/filter.json?track=$twitterTrackWord",
      headers = List(RawHeader("Authorization", authorizationHeader))
    )

    // Elastic Search settings
    val esClusterName = appConf.getString("elastic-search.cluster.name")
    val settings = Settings.settingsBuilder().put("cluster.name", esClusterName).build()

    // set the Elastic Search Cluster Connection
    val esClientURI = appConf.getString("elastic-search.client.uri")
    val esClient = ElasticClient.transport(settings, esClientURI)

    // create and distribute the workload among many akka Actors using a router
    val saveInESActor = system.actorOf(Props(new SaveInESActor(esClient, twitterTrackWord)).withRouter(RoundRobinPool(parallelism_level)))


    // Akka Stream

    // flow used to split the data bytes received and get singular tweets
    val chunkBuffer: Flow[ByteString, ByteString, NotUsed] = Framing.delimiter(
      ByteString("\r\n"),
      Int.MaxValue,
      false
    )

    val source: Source[HttpRequest, NotUsed] = Source.single(request)
    val sink: Sink[Any, Future[Done]] = Sink.foreach( rawTweet => saveInESActor ! rawTweet)
    // create the akka Stream Graph
    val graph: RunnableGraph[NotUsed] =
      source
        .via(connectionFlow)
        .flatMapConcat { resp =>
            resp.entity.dataBytes
        }
        .via(chunkBuffer)
        .map(_.utf8String)
        .filter(tweet => tweet.contains("text"))
        .to(sink)

    // materializer is implicitly used here
    graph.run()

  }

}
