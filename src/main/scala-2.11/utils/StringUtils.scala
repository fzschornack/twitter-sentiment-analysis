package utils

/**
  * Created by fzschornack on 18/08/2016.
  */
object StringUtils {

  def onlyWords(text: String) : String = {
    text.replaceAll("""(,|\.|'|!|\?|RT|\\n)"""," ").split(" ").filter(_.matches("""\w+""")).fold("")((a,b) => a + " " + b).trim.toLowerCase
  }

  def extractHashTags(text: String) : List[String] = {
    text.replaceAll("(O|o)lympics2016","").split(" ").filter(_.matches("""#(\w+)""")).toList
  }

  def extractHour(dateTime: String) : String = {
    dateTime.dropRight(16)
  }

  def extractDay(dateTime: String) : String = {
    dateTime.dropRight(19)
  }

}
