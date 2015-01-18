package ch.uzh.ifi.mamato.crowdPdf.hcomp.crowdpdf

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties
import ch.uzh.ifi.pdeboer.pplib.hcomp.crowdflower.CFQuery
import ch.uzh.ifi.pdeboer.pplib.util._
import play.api.libs.json.{Json, JsValue}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Created by Mattia on 15.01.2015.
 */

class CPURLBuilder(restMethod: String) extends URLBuilder("http", "localhost", 80, restMethod)

abstract class CPJobBase(apiKey: String) extends LazyLogger {
  protected val jobResourceJSONUrl = new CPURLBuilder("jobs.json")

  protected def sendAndAwaitJson(request: RESTClient, timeout: Duration) = {
    val body = Await.result(request.responseBody, timeout)
    val statusCode: Int = body.statusCode
    if (!body.isOk) {
      val error = new IllegalStateException(s"got NON-OK status return code: $statusCode with body ${body.body}")
      logger.error("err", error)
      throw error
    }

    val json: JsValue = Json.parse(body.body)
    logger.debug(s"got data $json")
    json
  }

  class CPJobStatusManager(apiKey: String, jobId: Int) extends CPJobBase(apiKey) {

    def cancelQuery(maxTries: Int = 3): Unit = {
      logger.info(s"$jobId : cancelling task")
      val cancelURL = jobIdResourceURL / "cancel"
      val request = new PUT(cancelURL.addQueryParameter("key", apiKey) + "")
      request.headers += "Content-Type" -> "application/x-www-form-urlencoded"

      U.retry(maxTries) {
        sendAndAwaitJson(request, Duration(30, "seconds"))
      }
    }

    def jobIdResourceURL = new CPURLBuilder("jobs") / s"$jobId"
  }

}

class CPJobCreator(apiKey: String, query: CFQuery, properties: HCompQueryProperties, sandbox: Boolean = false) extends CPJobBase(apiKey) {

}