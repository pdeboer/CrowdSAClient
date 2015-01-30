package ch.uzh.ifi.mamato.crowdPdf.hcomp.crowdpdf

import ch.uzh.ifi.mamato.crowdPdf.model.{Answer, Question}
import ch.uzh.ifi.mamato.crowdPdf.persistence.QuestionDAO
import ch.uzh.ifi.mamato.crowdPdf.util.{HttpRestClient, LazyLogger}
import java.util.Date
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQueryProperties, HCompQuery}
import ch.uzh.ifi.pdeboer.pplib.util.GrowingTimer
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.message.BasicNameValuePair
import org.apache.http.{NameValuePair, HttpEntity}
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.util.EntityUtils
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, Seconds}

import scala.collection
import scala.collection.parallel.mutable
import scala.concurrent.duration._

/**
 * Created by Mattia on 20.01.2015.
 */
class CrowdPdfManager(val service: CrowdPdfService, val qu: HCompQuery, val properties: CrowdPdfQueryProperties) extends LazyLogger {

  var questionId : Long= 0
  var cancelled: Boolean = false

  def waitForResponse() : Option[HCompAnswer] = {
    def durationIn(unit: TimeUnit): FiniteDuration = {
      durationIn(SECONDS)
    }

    val timer = new GrowingTimer(1 second, 1.0001, 20 seconds)
    //very very ugly, but we dont have a break statement in scala..
    var answer: Option[HCompAnswer] = None
    try {
      (1 to 100000).view.foreach(i => {
        answer = poll()
        if (cancelled || answer.isDefined) throw new Exception("I'm actually not an Exception")
        timer.waitTime
      })
    }
    catch {
      case e: Exception => {
        /*hopefully we land here*/
      }
    }
    answer
  }

  /**
   * @return HIT ID
   */
  def createQuestion() : Long = {
    questionId = service.CreateQuestion(qu.question, properties)
    questionId
  }

  def cancelQuestion(): Unit = {
    service.DisableQuestion(questionId)
    cancelled = true
  }

  def poll(): Option[HCompAnswer] = {
    logger.debug("checking answer for questionId: " + questionId+ "..")
		val answers = service.GetAnswersForQuestion(questionId)
		answers.headOption match {
      case None => None
      case Some(a: Answer) => handleAnswerResult(a)
		}
	}

  def handleAnswerResult(a: Answer): Option[HCompAnswer] = {
    logger.debug("Got an answer: " + a.answer)
    try {
      //We approve all NON EMPTY answers by default.
      if(a.answer != null && a.answer!="") {
        service.ApproveAnswer(a)
      } else {
        service.RejectAnswer(a)
      }
      val answer = new HCompAnswer {
        override def query: HCompQuery = qu
      }

      //TODO: correct this
      answer.acceptTime = Option(new DateTime(new Date(a.completedTime)))
      answer.submitTime = Option(new DateTime(new Date(a.completedTime)))
      return Option(answer)
    }
    catch {
      case e: Exception => logger.error("could not approve assignment", e)
    }
    return null
  }

}
