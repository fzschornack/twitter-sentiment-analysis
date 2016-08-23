package models

/**
  *
  * @param startDate
  * @param endDate
  * @param text
  * @param operator
  */
case class QueryParameters(startDate: Option[Long],
                           endDate: Option[Long],
                           text: Option[String],
                           operator: String)
