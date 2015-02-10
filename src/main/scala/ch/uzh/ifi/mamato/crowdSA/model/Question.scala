package ch.uzh.ifi.mamato.crowdSA.model

import ch.uzh.ifi.mamato.crowdSA.persistence.QuestionDAO
import scalikejdbc.DBSession

/**
 * Created by Mattia on 24.12.2014.
 */

case class Question(id: Long, question: String, question_type: String, reward_cts: Int, created_at: Long,
                    remote_paper_id: Long, remote_question_id: Long, disabled: Boolean, maximal_assignments: Int,
                     expiration_time_sec: Long)
