package ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa

import ch.uzh.ifi.mamato.crowdSA.model.Highlight
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties

/**
 * Created by Mattia on 30.01.2015.
 */
class CrowdSAQueryProperties(val paper_id: Long, val question_type: String,
                             val highlight: Highlight, paymentCents: Int, val expiration_time_sec: Long,
                             val maximal_assignments: Int, val possible_answers: Option[String] = Some(""),
                             val deniedTurkers: Option[List[Long]] ) extends HCompQueryProperties with Serializable
