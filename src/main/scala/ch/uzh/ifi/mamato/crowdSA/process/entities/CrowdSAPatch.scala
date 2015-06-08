package ch.uzh.ifi.mamato.crowdSA.process.entities

import java.util.Date

import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch

import scala.collection.mutable

/**
 * Created by mattia on 08.06.15.
 */
class CrowdSAPatch(val question: String, val questionType: String, val terms: String, val paperId: Long,
                   val assumption: String, val methodList: mutable.HashMap[String, mutable.MutableList[String]] = null,
                  val possibleAnswers: Option[String] = Some(""), var dataset: String = "",
                   var deniedTurkers: Option[List[Long]] = null,
                   var remoteQuestionId: Long = -1,
                    val rewardCts: Int = 10,
                   val expirationTimeSec: Long = ((new Date().getTime() / 1000) + 60 * 60 * 24 * 365),
                  var answer: String = "",
                  var answerId: Long = -1
                    )
  extends Patch(question) {

  override def canEqual(other: Any): Boolean = other.isInstanceOf[CrowdSAPatch]

  override def duplicate(value: String, payload: Option[_ <: Serializable] = this.payload) = {
    val p = new CrowdSAPatch(question, questionType, terms, paperId, assumption, methodList, possibleAnswers,
      dataset, deniedTurkers, remoteQuestionId, rewardCts, expirationTimeSec, answer, answerId)

    p.auxiliaryInformation = auxiliaryInformation
    p
  }
}
