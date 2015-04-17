package ch.uzh.ifi.mamato.crowdSA.model

/**
 * Created by mattia on 28.02.15.
 */

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Dataset(id: Long, statistical_method: String, dom_children: String, name: String, url: Option[String])

object Dataset {
  implicit val datasetWrites = new Writes[Dataset] {
    def writes(a: Dataset): JsValue = {
      Json.obj(
        "id" -> a.id,
        "statistical_method" -> a.statistical_method,
        "dom_children" -> a.dom_children,
        "name" -> a.name,
        "url" -> a.url
      )
    }
  }

  val datasetReadsBuilder =
    (JsPath \ "id").read[Long] and
      (JsPath \ "statistical_method").read[String] and
      (JsPath \ "dom_children").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "url").read[Option[String]]

  implicit val datasetReads = datasetReadsBuilder.apply(Dataset.apply _)
}