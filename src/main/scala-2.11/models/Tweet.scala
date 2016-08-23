package models

/**
  *
  * @param timestamp_ms
  * @param id_str
  * @param text
  * @param retweet_count
  * @param favorite_count
  * @param user
  */
case class Tweet(timestamp_ms: String,
                 id_str: String,
                 text: String,
                 retweet_count: Int,
                 favorite_count: Int,
                 user: User)
