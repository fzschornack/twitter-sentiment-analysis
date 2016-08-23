package actors

import akka.actor.Actor
import akka.event.Logging
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.JsonDocumentSource
import models._
import spray.json.{JsonParser, _}

/**
  * Actor that converts a tweet to json and
  * then stores it in Elastic Search
  *
  * @param client ElasticClient used to access Elastic Search
  * @param esType Type where tweets will be stored
  *
  * Created by fzschornack on 13/08/2016.
  */
class SaveInESActor(client: ElasticClient, esType: String) extends Actor with JsonSupport {

  val log = Logging(context.system, this)

  def receive: Receive = {
    case rawTweet: String =>
      val tweet = JsonParser(rawTweet).convertTo[Tweet]
      log.info(tweet.toString)
      client.execute{index into "tweets_2" -> esType  doc JsonDocumentSource(tweet.toJson.compactPrint)}
  }

}
