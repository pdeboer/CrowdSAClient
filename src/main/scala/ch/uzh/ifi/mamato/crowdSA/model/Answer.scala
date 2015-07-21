package ch.uzh.ifi.mamato.crowdSA.model

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQuery}
import ch.uzh.ifi.pdeboer.pplib.patterns.pruners.Prunable
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

/**
 * Created by Mattia on 04.01.2015.
 */

case class Answer(id: Long, answer: String, created_at: Long, is_method_used: Boolean,
                  accepted: Option[Boolean], bonus_cts: Option[Int],
                  rejected: Option[Boolean], assignments_id: Long, accuracy: String) extends HCompAnswer with Serializable with Prunable {
  override def toString() = answer

  override def query: HCompQuery = null

  override def processingTimeMillis: Long = submitTime.getOrElse(receivedTime).getMillis -
    acceptTime.getOrElse(new DateTime(created_at)).getMillis

  override def prunableDouble = processingTimeMillis.toDouble

}


object Answer {

  implicit val answerWrites = new Writes[Answer] {
    def writes(a: Answer): JsValue = {
      Json.obj(
        "id" -> a.id,
        "answer" -> a.answer,
        "created_at" -> a.created_at,
        "is_method_used" -> a.is_method_used,
        "accepted" -> a.accepted,
        "bonus_cts" -> a.bonus_cts,
        "rejected" -> a.rejected,
        "assignments_id" -> a.assignments_id,
        "accuracy" -> a.accuracy
      )
    }
  }

  val answerReadsBuilder =
    (JsPath \ "id").read[Long] and
      (JsPath \ "answer").read[String] and
      (JsPath \ "created_at").read[Long] and
      (JsPath \ "is_method_used").read[Boolean] and
      (JsPath \ "accepted").read[Option[Boolean]] and
      (JsPath \ "bonus_cts").read[Option[Int]] and
      (JsPath \ "rejected").read[Option[Boolean]] and
      (JsPath \ "assignments_id").read[Long] and
      (JsPath \ "accuracy").read[String]

  implicit val answerReads = answerReadsBuilder.apply(Answer.apply _)

}