package ch.uzh.ifi.mamato.crowdSA.model

import ch.uzh.ifi.mamato.crowdSA.persistence.PaperDAO
import scalikejdbc._
import org.joda.time.DateTime

/**
 * Created by Mattia on 19.01.2015.
 *
 * Based on:
 * https://github.com/scalikejdbc/devteam-app/blob/master/src/main/scala/devteam/model/Company.scala
 */
case class Paper(
                  id: Long,
                  title: String,
                  budget: Int,
                  remote_id: Long) {
  def save()(implicit session: DBSession = PaperDAO.autoSession): Paper = PaperDAO.save(this)(session)
  //def destroy()(implicit session: DBSession = Paper.autoSession): Unit = Paper.destroy(id)(session)
}