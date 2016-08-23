package sentiment

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations
import models.Sentiment._
import models.Sentiment

import scala.collection.convert.wrapAll._

/**
  * Stanford Core NLP sentiment analysis.
  * More information: http://stanfordnlp.github.io/CoreNLP/
  */
object SentimentAnalyzer {

  /**
    * Process the text and compute the sentiments using the StanfordCoreNLP pipeline
    *
    * @param text
    * @return List of String and Sentiments
    */
  def extractSentiments(text: String, pipeline: StanfordCoreNLP): List[(String, Sentiment)] = {
    val annotation: Annotation = pipeline.process(text)
    val sentences = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
    sentences
      .map(sentence => (sentence, sentence.get(classOf[SentimentCoreAnnotations.SentimentAnnotatedTree])))
      .map { case (sentence, tree) => (sentence.toString, Sentiment.toSentiment(RNNCoreAnnotations.getPredictedClass(tree))) }
      .toList
  }

  /**
    * Extracts the 'main' sentiment
    *
    * @param text
    * @return Sentiment
    */
  private def extractSentiment(text: String, pipeline: StanfordCoreNLP): Sentiment = {
    val (_, sentiment) = extractSentiments(text, pipeline)
      .maxBy { case (sentence, _) => sentence.length }
    sentiment
  }

  /**
    * Returns the 'main' sentiment or NOT_UNDERSTOOD
    *
    * @param input
    * @return Sentiment
    */
  def mainSentiment(input: String, pipeline: StanfordCoreNLP): Sentiment = Option(input) match {
    case Some(text) if !text.isEmpty => extractSentiment(text, pipeline)
    case _ => Sentiment.NOT_UNDERSTOOD
  }

}