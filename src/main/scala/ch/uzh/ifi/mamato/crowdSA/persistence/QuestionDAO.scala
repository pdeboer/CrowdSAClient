package ch.uzh.ifi.mamato.crowdSA.persistence

import ch.uzh.ifi.mamato.crowdSA.model.{Assumption, Question}
import ch.uzh.ifi.mamato.crowdSA.persistence.AssumptionsDAO._
import scalikejdbc._

/**
 * Created by Mattia on 19.01.2015.
 */
object QuestionDAO extends SQLSyntaxSupport[Question] {

  override val tableName = "questions"

  def apply(q: SyntaxProvider[Question])(rs: WrappedResultSet): Question = apply(q.resultName)(rs)
  def apply(q: ResultName[Question])(rs: WrappedResultSet): Question =
    new Question(rs.long(q.id), rs.string(q.question), rs.string(q.question_type), rs.int(q.reward_cts),
      rs.long(q.created_at), rs.long(q.remote_paper_id), rs.long(q.remote_question_id), rs.boolean(q.disabled),
      rs.int(q.maximal_assignments), rs.long(q.expiration_time_sec), rs.stringOpt(q.possible_answers))

  val q = QuestionDAO.syntax("q")

  def find(id: Long)(implicit session: DBSession = autoSession): Option[Question] = withSQL {
    select.from(QuestionDAO as q).where.eq(q.id, id)//.and.append(isNotDeleted)
  }.map(QuestionDAO(q)).single.apply()

  def findAll()(implicit session: DBSession = autoSession): List[Question] = withSQL {
    select.from(QuestionDAO as q)
      //.where.append(isNotDeleted)
      .orderBy(q.id)
  }.map(QuestionDAO(q)).list.apply()

  def findAllEnabled()(implicit session: DBSession = autoSession): Iterable[Question] = withSQL {
    select.from(QuestionDAO as q)
    .where.eq(q.disabled, false)
  }.map(QuestionDAO(q)).list.apply()

  def getByRemoteQuestionId(rId: Long)(implicit session: DBSession = autoSession): Option[Question] = withSQL {
    select.from(QuestionDAO as q)
      .where.eq(q.remote_question_id, rId)
  }.map(QuestionDAO(q)).single.apply()

  def countAll()(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(QuestionDAO as q)//.where.append(isNotDeleted)
  }.map(rs => rs.long(1)).single.apply().get

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Question] = withSQL {
    select.from(QuestionDAO as q).where.append(sqls"${where}")
      //.where.append(isNotDeleted)
      .orderBy(q.id)
  }.map(QuestionDAO(q)).list.apply()

  def findByRemoteId(remoteId: Long)(implicit session: DBSession = autoSession): Option[Question] = withSQL {
    select.from(QuestionDAO as q).where.eq(q.remote_question_id, remoteId)//.and.append(isNotDeleted)
  }.map(QuestionDAO(q)).single.apply()

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(QuestionDAO as q).where.append(sqls"${where}")//.and.append(isNotDeleted)
  }.map(_.long(1)).single.apply().get


  def create(question: String, question_type: String, reward_cts: Int, created_at: Long, remote_paper_id: Long,
             remote_question_id: Long, maximal_assignments: Int, expiration_time_sec: Long, possible_answers: Option[String])
            (implicit session: DBSession = autoSession): Long = {
    try {
      val id = withSQL {
        insert.into(QuestionDAO).namedValues(
          column.question -> question,
          column.question_type -> question_type,
          column.reward_cts -> reward_cts,
          column.created_at -> created_at,
          column.remote_paper_id -> remote_paper_id,
          column.remote_question_id -> remote_question_id,
          column.maximal_assignments -> maximal_assignments,
          column.expiration_time_sec -> expiration_time_sec,
          column.possible_answers -> possible_answers,
          column.disabled -> false
        )
      }.updateAndReturnGeneratedKey.apply()
      return id
    }catch {
      case e: Exception => e.printStackTrace()
    }
    -1
  }

  def save(question_id: Long, disabled: Boolean, expiration_time_sec: Long, maximal_assignments: Int)
          (implicit session: DBSession = autoSession) = {
    withSQL {
      update(QuestionDAO).set(
        column.disabled -> disabled, column.expiration_time_sec -> expiration_time_sec
        , column.maximal_assignments -> maximal_assignments
      ).where.eq(column.id, question_id)
    }.update.apply()
  }

}