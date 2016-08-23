package models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

/**
  * Created by fzschornack on 22/08/2016.
  */
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val userFormat = jsonFormat2(User)
  implicit val tweetFormat = jsonFormat6(Tweet)
  implicit val tweetWithSentimentFormat = jsonFormat3(TweetWithSentiment)
  implicit val hashtagSentimentCountFormat = jsonFormat2(HashtagSentimentCount)
  implicit val dailyTrendTopicFormat = jsonFormat4(DailyTrendTopic)
  implicit val hourlyTrendTopicFormat = jsonFormat4(HourlyTrendTopic)
  implicit val sentimentAnalysisResponseFormat = jsonFormat3(SentimentAnalysisResponse)
  implicit val queryParametersFormat = jsonFormat4(QueryParameters)
}
