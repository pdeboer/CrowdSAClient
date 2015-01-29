package ch.uzh.ifi.mamato.crowdPdf.model

import play.api.libs.json._
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ // Combinator syntax

/**
 * Created by Mattia on 04.01.2015.
 */


private[crowdPdf] case class Answer(id: Long, answer: String, completedTime: Long, accepted: Option[Boolean], acceptedAndBonus: Option[Boolean], rejected: Option[Boolean], assignment_fk: Long) extends Serializable

object Answer {
  implicit val answerWrites = new Writes[Answer] {
    def writes(a: Answer): JsValue = {
      Json.obj(
        "id" -> a.id,
        "answer" -> a.answer,
        "completedTime" -> a.completedTime,
        "accepted" -> a.accepted,
        "acceptedAndBonus" -> a.acceptedAndBonus,
        "rejected" -> a.rejected,
        "assignment_fk" -> a.assignment_fk
      )
    }
  }

  val answerReadsBuilder =
    (JsPath \ "id").read[Long] and
      (JsPath \ "answer").read[String] and
      (JsPath \ "completedTime").read[Long] and
      (JsPath \ "accepted").read[Option[Boolean]] and
      (JsPath \ "acceptedAndBonus").read[Option[Boolean]] and
      (JsPath \ "rejected").read[Option[Boolean]] and
      (JsPath \ "assignment_fk").read[Long]

  implicit val answerReads = answerReadsBuilder.apply(Answer.apply _)

}