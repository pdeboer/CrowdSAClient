package ch.uzh.ifi.mamato.crowdSA.model

import ch.uzh.ifi.mamato.crowdSA.persistence.{StatMethodsDAO, PaperDAO}
import scalikejdbc._

/**
 * Created by Mattia on 19.01.2015.
 */
case class StatMethod(
                  id: Long,
                  stat_method: String) {
  def save()(implicit session: DBSession = StatMethodsDAO.autoSession): StatMethod = StatMethodsDAO.save(this)(session)
}