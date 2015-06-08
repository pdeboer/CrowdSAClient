package ch.uzh.ifi.mamato.crowdSA.process.entities

import ch.uzh.ifi.pdeboer.pplib.process.entities.Patch

/**
 * Created by mattia on 08.06.15.
 */
class CrowdSAPatch(question: String) extends Patch(question) {

  override def canEqual(other: Any): Boolean = other.isInstanceOf[CrowdSAPatch]

  override def duplicate(value: String, payload: Option[_ <: Serializable] = this.payload) = {
    val p = new CrowdSAPatch(value)
    p.auxiliaryInformation = auxiliaryInformation
    p
  }
}
