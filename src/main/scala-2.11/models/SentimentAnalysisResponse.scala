package models

/**
  *
  * @param tweetsWithSentiment
  * @param dailyTrendTopics
  * @param hourlyTrendTopics
  */
case class SentimentAnalysisResponse(tweetsWithSentiment: Array[TweetWithSentiment],
                                     dailyTrendTopics: Array[DailyTrendTopic],
                                     hourlyTrendTopics: Array[HourlyTrendTopic])
