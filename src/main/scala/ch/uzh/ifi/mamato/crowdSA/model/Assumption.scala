package ch.uzh.ifi.mamato.crowdSA.model

import ch.uzh.ifi.mamato.crowdSA.persistence.StatMethodsDAO
import scalikejdbc.DBSession

/**
 * Created by mattia on 19.03.15.
 */
case class Assumption(
                       id: Long,
                       assumption: String) {
}
