package ch.uzh.ifi.mamato.crowdPdf.model

import ch.uzh.ifi.mamato.crowdPdf.persistence.QuestionDAO
import scalikejdbc.DBSession

/**
 * Created by Mattia on 24.12.2014.
 */

case class Question(id: Long, question: String, questiontype: String, reward: Int, created: Long, paper_fk: Long, question_id: Long){
  def save()(implicit session: DBSession = QuestionDAO.autoSession): Question = QuestionDAO.save(this)(session)
}
