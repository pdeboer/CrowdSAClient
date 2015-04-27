package ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa

import java.util.Date

import ch.uzh.ifi.mamato.crowdSA.model.Answer
import ch.uzh.ifi.mamato.crowdSA.persistence.StatMethodsDAO
import ch.uzh.ifi.mamato.crowdSA.util.LazyLogger
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.util.Random

/**
 * Created by Mattia on 20.01.2015.
 */
class CrowdSAManager(val service: CrowdSAService, val qu: CrowdSAQuery) extends LazyLogger {

  var questionId : Long= 0
  var cancelled: Boolean = false

  def waitForResponse() : Option[Answer] = {
    def durationIn(unit: TimeUnit): FiniteDuration = {
      durationIn(SECONDS)
    }

    var answer: Option[Answer] = None
    try {
      (1 to 100000).view.foreach(i => {
        Thread.sleep(ConfigFactory.load("application.conf").getInt("pollTimeMS"))

        avoidDBConnectionTimeout()

        answer = poll()
        if (cancelled || answer.isDefined){
          throw new scala.Exception("I'm actually not an Exception")
        }
      })
    }
    catch {
      case e: Exception => {
        /*hopefully we land here*/
      }
    }
    answer
  }

  private def avoidDBConnectionTimeout(): Unit = {
    if (Random.nextDouble() < 0.1) {
      StatMethodsDAO.findAll()
    }
  }

  /**POST a question to the server
   * @return question id
   */
  def createQuestion() : Long = {
    questionId = service.CreateQuestion(qu)
    questionId
  }

  def cancelQuestion(): Unit = {
    service.DisableQuestion(questionId)
    cancelled = true
  }

  def poll(): Option[Answer] = {
		val answers = service.GetAnswersForQuestion(questionId)
		answers.lastOption match {
      case None => None
      case Some(a: Answer) => handleAnswerResult(a)
		}
	}

  def handleAnswerResult(a: Answer): Option[Answer] = {
    logger.debug("Got an answer: " + a.answer)
    try {
      //We approve all NON EMPTY answers by default.
      if(a.answer != null && a.answer!="") {
        service.ApproveAnswer(a)

      } else {
        service.RejectAnswer(a)
      }

      a.acceptTime = Option(new DateTime(new Date(service.getAssignmentForAnswerId(a.id).created_at)))
      a.submitTime = Option(new DateTime(new Date(a.created_at)))

      Some(a)
    }
    catch {
      case e: Exception => {
        logger.error("could not approve assignment", e)
        None
      }
    }
  }

}
