package models

/**
  *
  * @param day
  * @param hashtag
  * @param sentimentCounts
  * @param total
  */
case class DailyTrendTopic(day: String,
                           hashtag: String,
                           sentimentCounts: Array[HashtagSentimentCount],
                           total: Int)
