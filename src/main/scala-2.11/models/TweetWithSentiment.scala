package models

/**
  *
  * @param dateTime
  * @param text
  * @param sentiment
  */
case class TweetWithSentiment(dateTime: String,
                              text: String,
                              sentiment: String)
