package ch.uzh.ifi.mamato.crowdPdf.hcomp

import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQuery

/**
 * Created by Mattia on 14.01.2015.
 */

@SerialVersionUID(1l)
case class CrowdPdfQuery(question: String, defaultAnswer: String = "", title: String = "") extends HCompQuery with Serializable {
  def this(question: String, defaultAnswer: String) = this(question, defaultAnswer, question)

  def this(question: String) = this(question, "", question)

  private var _valueIsRequired: Boolean = defaultAnswer.equals("")

  override def valueIsRequired = _valueIsRequired

  def setRequired(required: Boolean) = {
    _valueIsRequired = required
    this
  }

  override def suggestedPaymentCents: Int = 8
}

object CrowdPdfQuery {
  def apply(question: String, defaultAnswer: String): CrowdPdfQuery = apply(question, defaultAnswer, question)

  def apply(question: String): CrowdPdfQuery = apply(question, "", question)
}