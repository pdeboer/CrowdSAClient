package ch.uzh.ifi.mamato.crowdSA.model

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

/**
 * Created by Mattia on 04.01.2015.
 */


private[crowdSA] case class Answer(id: Long, answer: String, created_at: Long,
                                   accepted: Option[Boolean], bonus_cts: Option[Int],
                                   rejected: Option[Boolean], assignments_id: Long) extends Serializable

object Answer {
  implicit val answerWrites = new Writes[Answer] {
    def writes(a: Answer): JsValue = {
      Json.obj(
        "id" -> a.id,
        "answer" -> a.answer,
        "created_at" -> a.created_at,
        "accepted" -> a.accepted,
        "bonus_cts" -> a.bonus_cts,
        "rejected" -> a.rejected,
        "assignments_id" -> a.assignments_id
      )
    }
  }

 /* implicit val answerRead : Reads[Answer] =  (
    (JsPath \ "id").read[Long] and
      (JsPath \ "answer").read[String] and
      (JsPath \ "created_at").read[Long] and
      (JsPath \ "accepted").read[Option[Boolean]] and
      (JsPath \ "bonus_cts").read[Option[Int]] and
      (JsPath \ "rejected").read[Option[Boolean]] and
      (JsPath \ "assignments_id").read[Long]

    )(Answer.apply _)*/

  val answerReadsBuilder =
    (JsPath \ "id").read[Long] and
      (JsPath \ "answer").read[String] and
      (JsPath \ "created_at").read[Long] and
      (JsPath \ "accepted").read[Option[Boolean]] and
      (JsPath \ "bonus_cts").read[Option[Int]] and
      (JsPath \ "rejected").read[Option[Boolean]] and
      (JsPath \ "assignments_id").read[Long]

  implicit val answerReads = answerReadsBuilder.apply(Answer.apply _)


}