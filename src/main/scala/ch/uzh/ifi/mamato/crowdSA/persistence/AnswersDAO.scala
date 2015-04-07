package ch.uzh.ifi.mamato.crowdSA.persistence

import ch.uzh.ifi.mamato.crowdSA.model.Answer
import scalikejdbc._

/**
 * Created by mattia on 19.03.15.
 */
object AnswersDAO extends SQLSyntaxSupport[Answer] {
  override val tableName = "Answers"

  def apply(p: SyntaxProvider[Answer])(rs: WrappedResultSet): Answer = apply(p.resultName)(rs)
  def apply(p: ResultName[Answer])(rs: WrappedResultSet): Answer = new Answer(rs.long(p.id), rs.string(p.answer), rs.long(p.created_at), rs.booleanOpt(p.accepted),rs.intOpt(p.bonus_cts), rs.booleanOpt(p.rejected), rs.long(p.assignments_id))

  val p = AnswersDAO.syntax("p")


  def find(id: Long)(implicit session: DBSession = autoSession): Option[Answer] = withSQL {
    select.from(AnswersDAO as p).where.eq(p.id, id)//.and.append(isNotDeleted)
  }.map(AnswersDAO(p)).single.apply()

  def findByAnswer(answer: String)(implicit session: DBSession = autoSession): Option[Answer] = withSQL {
    select.from(AnswersDAO as p)
      .where.eq(p.answer, answer)
      .orderBy(p.id)
  }.map(AnswersDAO(p)).single.apply()

  def findAll()(implicit session: DBSession = autoSession): List[Answer] = withSQL {
    select.from(AnswersDAO as p)
      .orderBy(p.id)
  }.map(AnswersDAO(p)).list.apply()

  def countAll()(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(AnswersDAO as p)//.where.append(isNotDeleted)
  }.map(rs => rs.long(1)).single.apply().get

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Answer] = withSQL {
    select.from(AnswersDAO as p).where.append(sqls"${where}")
      .orderBy(p.id)
  }.map(AnswersDAO(p)).list.apply()

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(AnswersDAO as p).where.append(sqls"${where}")//.and.append(isNotDeleted)
  }.map(_.long(1)).single.apply().get

  def create(answer: Answer)(implicit session: DBSession = autoSession): Answer = {
    withSQL {
      insert.into(AnswersDAO).namedValues(
        column.id -> answer.id,
        column.answer -> answer.answer,
        column.created_at -> answer.created_at,
        column.accepted -> answer.accepted,
        column.bonus_cts -> answer.bonus_cts,
        column.rejected -> answer.rejected,
        column.assignments_id -> answer.assignments_id)
    }.executeUpdate().apply()
    find(answer.id).get
  }

}
