package ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQuery}

/**
 * Created by mattia on 09.03.15.
 */
case class CrowdSAQuery(question: String, suggestedPaymentCents: Int, properties: CrowdSAQueryProperties) extends HCompQuery with Serializable {

  override def title: String = null
}
