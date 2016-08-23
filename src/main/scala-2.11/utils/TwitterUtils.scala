package utils

import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.parboiled2.util.Base64
import com.typesafe.config._

/**
  * Create an Oauth authorization header
  * to be used on Twitter Stream API requests
  * More information: https://dev.twitter.com/oauth/overview/authorizing-requests
  *
  *
  * Created by fzschornack on 22/08/2016.
  */
object TwitterUtils {

  val conf = ConfigFactory.load()

  def getAuthorizationHeader(method: String, url: String, urlParameters: String): String = {
    val oauth_consumer_key = conf.getString("twitter.auth.consumer-key")
    val oauth_nonce = System.nanoTime.toString
    val oauth_signature_method = "HMAC-SHA1"
    val oauth_timestamp = System.currentTimeMillis()/1000
    val oauth_token = conf.getString("twitter.auth.access-token")
    val oauth_version = "1.0"


    val parameterString = generateParameterString(oauth_consumer_key,
      oauth_nonce,
      oauth_signature_method,
      oauth_timestamp.toString,
      oauth_token,
      oauth_version,
      urlParameters
    )

    val oauth_signature = generateOauthSignature(method, url, parameterString)

    return s"""OAuth oauth_consumer_key="${URLEncoder.encode(oauth_consumer_key, "UTF-8")}", oauth_nonce="${URLEncoder.encode(oauth_nonce, "UTF-8")}", oauth_signature="${URLEncoder.encode(oauth_signature, "UTF-8")}", oauth_signature_method="${URLEncoder.encode(oauth_signature_method, "UTF-8")}", oauth_timestamp="${URLEncoder.encode(oauth_timestamp.toString, "UTF-8")}", oauth_token="${URLEncoder.encode(oauth_token, "UTF-8")}", oauth_version="${URLEncoder.encode(oauth_version, "UTF-8")}""""

  }

  def generateOauthSignature(method: String, url: String, parameterString: String): String = {

    val signature_base_string =
      s"""${method.toUpperCase}&${URLEncoder.encode(url, "UTF-8")}&${URLEncoder.encode(parameterString, "UTF-8")}"""

    val consumer_secret = conf.getString("twitter.auth.consumer-secret")
    val oauth_token_secret = conf.getString("twitter.auth.access-token-secret")

    val signing_key =
      s"""${URLEncoder.encode(consumer_secret, "UTF-8")}&${URLEncoder.encode(oauth_token_secret, "UTF-8")}"""

    val secret = new SecretKeySpec(signing_key.getBytes, "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")

    mac.init(secret)
    val oauth_signature = Base64.rfc2045().encodeToString(mac.doFinal(signature_base_string.getBytes), false)
    mac.reset()

    return oauth_signature
  }

  def generateParameterString(oauth_consumer_key: String,
                              oauth_nonce: String,
                              oauth_signature_method: String,
                              oauth_timestamp: String,
                              oauth_token: String,
                              oauth_version: String,
                              urlParameters: String): String = {

    return s"""oauth_consumer_key=${URLEncoder.encode(oauth_consumer_key, "UTF-8")}&oauth_nonce=${URLEncoder.encode(oauth_nonce, "UTF-8")}&oauth_signature_method=${URLEncoder.encode(oauth_signature_method, "UTF-8")}&oauth_timestamp=${URLEncoder.encode(oauth_timestamp, "UTF-8")}&oauth_token=${URLEncoder.encode(oauth_token, "UTF-8")}&oauth_version=${URLEncoder.encode(oauth_version, "UTF-8")}&${urlParameters}"""
  }

}
