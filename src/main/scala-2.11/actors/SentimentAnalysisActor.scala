package actors

import java.util.Properties

import akka.actor.Actor
import edu.stanford.nlp.pipeline.StanfordCoreNLP
import org.apache.spark.SparkContext
import org.elasticsearch.spark._
import org.joda.time.DateTime
import models._
import sentiment.SentimentAnalyzer
import utils.StringUtils

/**
  * Actor that uses Apache Spark to perform semantic analysis
  * on Elastic Search data
  *
  * @param sc SparkContext
  *
  * Created by fzschornack on 17/08/2016.
  */
class SentimentAnalysisActor(sc: SparkContext) extends Actor with JsonSupport {

  def receive: Receive = {
    case queryParameters: QueryParameters =>

      val now = System.currentTimeMillis()

      val queryWithText =
        s"""{
             "query": {
                 "bool": {
                     "must": [
                          {
                              "match" : {
                                  "text" : {
                                      "query" : "${queryParameters.text.get}",
                                      "operator" : "${queryParameters.operator}"
                                  }
                              }
                          },
                          {
                              "range" : {
                                  "timestamp_ms" : {
                                      "gte" : ${queryParameters.startDate.getOrElse(0)},
                                      "lt" : ${queryParameters.endDate.getOrElse(now)}
                                  }
                              }
                          }
                      ]
                 }
             }
        }"""

      val queryWithoutText =
        s"""{
             "query": {
                  "range" : {
                      "timestamp_ms" : {
                          "gte" : ${queryParameters.startDate.getOrElse(0)},
                          "lt" : ${queryParameters.endDate.getOrElse(now)}
                      }
                  }
             }
        }"""

      var query = queryWithoutText

      if (!queryParameters.text.getOrElse("").trim.isEmpty)
        query = queryWithText

      // create an RDD with data from Elastic Search
      val tweets_RDD = sc.esRDD("tweets_2/olympics2016", query)

      val processedTweets = tweets_RDD.mapPartitions { partition =>

        // define the properties to be used in the sentiment analysis pipeline
        val props = new Properties()
        props.setProperty("annotators", "tokenize, ssplit, parse, sentiment")

        // only one pipeline for each partition
        val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)

        partition.map { tweet =>
          val text = tweet._2("text").toString
          val textOnlyWords = StringUtils.onlyWords(text)
          val sentiment = SentimentAnalyzer.mainSentiment(textOnlyWords, pipeline).toString
          val timestamp = tweet._2("timestamp_ms").asInstanceOf[Long]
          val dateTime = new DateTime(timestamp).toString

          TweetWithSentiment(dateTime, text, sentiment)
        }
          .filter( tweetWithSentiment => !tweetWithSentiment.sentiment.equals(Sentiment.NOT_UNDERSTOOD.toString) )

      }
        .cache()

      val processedHashtags = processedTweets.flatMap { tweetWithSentiment =>
        val hashtags = StringUtils.extractHashTags(tweetWithSentiment.text)
        val day = StringUtils.extractDay(tweetWithSentiment.dateTime)
        val hour = StringUtils.extractHour(tweetWithSentiment.dateTime)

        hashtags.map( hashtag => (day, hour, hashtag, tweetWithSentiment.sentiment) )
      }
        .cache()

      val dailyTrendTopics = processedHashtags
        .map( ph => (ph._1, ph._3, ph._4) ) //(day, hashtag, sentiment)
        .map( triple => (triple, 1) )
        .reduceByKey(_ + _)
        .map { case ((day, hashtag, sentiment), count) =>
          ((day, hashtag), HashtagSentimentCount(sentiment, count))
        }
        .groupByKey()
        .map { case ((day, hashtag), sentimentCounts) =>
          val total = sentimentCounts.map(_.count).sum

          DailyTrendTopic(day, hashtag, sentimentCounts.toArray, total)
        }
        .top(10)(Ordering.by(_.total))

      val hourlyTrendTopics = processedHashtags
        .map( ph => (ph._2, ph._3, ph._4) ) //(hour, hashtag, sentiment)
        .map( triple => (triple, 1) )
        .reduceByKey(_ + _)
        .map { case ((hour, hashtag, sentiment), count) =>
          ((hour, hashtag), HashtagSentimentCount(sentiment, count))
        }
        .groupByKey()
        .map { case ((hour, hashtag), sentimentCounts) =>
            val total = sentimentCounts.map(_.count).sum

            HourlyTrendTopic(hour, hashtag, sentimentCounts.toArray, total)
        }
        .top(30)(Ordering.by(_.total))

      val tweetsWithSentiment = processedTweets.collect()

      processedHashtags.unpersist()
      processedTweets.unpersist()

      // send back a Response
      sender() ! SentimentAnalysisResponse(tweetsWithSentiment, dailyTrendTopics, hourlyTrendTopics)
  }

}
