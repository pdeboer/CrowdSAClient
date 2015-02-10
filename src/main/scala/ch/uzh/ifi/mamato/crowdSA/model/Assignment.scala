/**
 * Created by Mattia on 22.01.2015.
 */
package ch.uzh.ifi.mamato.crowdSA.model

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Assignment(id: Long, created_at: Long, expiration_time: Long, is_cancelled: Boolean, remote_question_id: Long, remote_team_id: Long)

object Assignment {
  implicit val assignmentWrites = new Writes[Assignment] {
    def writes(a: Assignment): JsValue = {
      Json.obj(
        "id" -> a.id,
        "created_at" -> a.created_at,
        "expiration_time" -> a.expiration_time,
        "is_cancelled" -> a.is_cancelled,
        "questions_id" -> a.remote_question_id,
        "teams_id" -> a.remote_team_id
      )
    }
  }

  val assignmentReadsBuilder =
    (JsPath \ "id").read[Long] and
      (JsPath \ "created_at").read[Long] and
      (JsPath \ "expiration_time").read[Long] and
      (JsPath \ "is_cancelled").read[Option[Boolean]] and
      (JsPath \ "questions_id").read[Option[Long]] and
      (JsPath \ "teams_id").read[Option[Long]]

  implicit val assignmentReads = assignmentReadsBuilder.apply(Assignment.apply _)

}