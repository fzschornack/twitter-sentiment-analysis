package models

/**
  *
  * @param hour
  * @param hashtag
  * @param sentimentCounts
  * @param total
  */
case class HourlyTrendTopic(hour: String,
                           hashtag: String,
                           sentimentCounts: Array[HashtagSentimentCount],
                           total: Int)
