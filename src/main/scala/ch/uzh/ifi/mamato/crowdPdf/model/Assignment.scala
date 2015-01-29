package ch.uzh.ifi.mamato.crowdPdf.model

/**
 * Created by Mattia on 22.01.2015.
 */
case class Assignment(id: Long, assignedFrom: Long, assignedTo: Long, acceptedTime: Long, question_fk: Long, team_fk: Long)