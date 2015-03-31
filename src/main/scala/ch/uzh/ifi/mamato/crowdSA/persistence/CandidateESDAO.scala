package ch.uzh.ifi.mamato.crowdSA.persistence

import ch.uzh.ifi.mamato.crowdSA.model.Dataset
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import scalikejdbc.{AutoSession, ConnectionPool, DB, _}

/**
 * Created by mattia on 28.02.15.
 */
object CandidateESDAO {
  val conf = ConfigFactory.load("application.conf")

  Class.forName(conf.getString("db.default.driver"))
  ConnectionPool.singleton(conf.getString("db.default.url"), conf.getString("db.default.user"), conf.getString("db.default.password"))
  implicit val session = AutoSession

  def candidates: List[ProcessCandidate] = DB readOnly { implicit session =>
    sql"select id, description, start_time, end_time, result, error, cost from discovery".map(rs => {
      val pc = ProcessCandidate(rs.long("id").toInt, rs.string("description"))
      pc.startTime = rs.jodaDateTimeOpt("start_time")
      pc.endTime = rs.jodaDateTimeOpt("end_time")
      pc.result = rs.stringOpt("result")
      pc.error = rs.stringOpt("error")
      pc.cost = rs.longOpt("cost").map(_.toInt)
      pc
    }).list().apply()
  }

  def insertProcessCandidate(c: ProcessCandidate): Unit = DB localTx { implicit session =>
    sql"INSERT INTO discovery (description) VALUES(${c.description})".update.apply()
  }

  def updateProcessCandidateTimes(c: ProcessCandidate): Unit = DB localTx { implicit session =>
    sql"""UPDATE discovery
			 SET start_time = ${c.startTime}, end_time = ${c.endTime},
	 				result = ${c.result}, error = ${c.error}, cost = ${c.cost}
			 WHERE id = ${c.id}""".update.apply()
  }
}

case class ProcessCandidate(id: Int, description: String) {
  var startTime: Option[DateTime] = None
  var endTime: Option[DateTime] = None
  var result: Option[String] = None
  var error: Option[String] = None
  var cost: Option[Int] = None
}