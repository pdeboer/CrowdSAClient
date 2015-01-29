package ch.uzh.ifi.mamato.crowdPdf.persistence

import ch.uzh.ifi.mamato.crowdPdf.model.Question
import scalikejdbc._

/**
 * Created by Mattia on 19.01.2015.
 */
object QuestionDAO extends SQLSyntaxSupport[Question] {

  override val tableName = "questions"

  def apply(q: SyntaxProvider[Question])(rs: WrappedResultSet): Question = apply(q.resultName)(rs)
  def apply(q: ResultName[Question])(rs: WrappedResultSet): Question =
    new Question(rs.long(q.id), rs.string(q.question), rs.string(q.questiontype), rs.int(q.reward), rs.long(q.created), rs.long(q.paper_fk), rs.long(q.question_id))

  val q = QuestionDAO.syntax("q")

  def find(id: Long)(implicit session: DBSession = autoSession): Option[Question] = withSQL {
    select.from(QuestionDAO as q).where.eq(q.id, id)//.and.append(isNotDeleted)
  }.map(QuestionDAO(q)).single.apply()

  def findAll()(implicit session: DBSession = autoSession): List[Question] = withSQL {
    select.from(QuestionDAO as q)
      //.where.append(isNotDeleted)
      .orderBy(q.id)
  }.map(QuestionDAO(q)).list.apply()

  def countAll()(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(QuestionDAO as q)//.where.append(isNotDeleted)
  }.map(rs => rs.long(1)).single.apply().get

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Question] = withSQL {
    select.from(QuestionDAO as q).where.append(sqls"${where}")
      //.where.append(isNotDeleted)
      .orderBy(q.id)
  }.map(QuestionDAO(q)).list.apply()

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(QuestionDAO as q).where.append(sqls"${where}")//.and.append(isNotDeleted)
  }.map(_.long(1)).single.apply().get


  def create(question: String, questiontype: String, reward: Int, created: Long, paper_fk: Long, question_id: Long)(implicit session: DBSession = autoSession): Long = {
    try {
      val id = withSQL {
        insert.into(QuestionDAO).namedValues(
          column.question -> question,
          column.questiontype -> questiontype,
          column.reward -> reward,
          column.created -> created,
          column.paper_fk -> paper_fk,
          column.question_id -> question_id
        )
      }.updateAndReturnGeneratedKey.apply()
      return id
    }catch {
      case e: Exception => e.printStackTrace()
    }
    -1
  }

  def save(m: Question)(implicit session: DBSession = autoSession): Question = {
    withSQL {
      update(QuestionDAO).set(
        column.question -> m.question, column.questiontype -> m.questiontype, column.reward -> m.reward, column.created -> m.created, column.paper_fk -> m.paper_fk, column.question_id -> m.question_id
      ).where.eq(column.id, m.id)//.and.isNull(column.deletedAt)
    }.update.apply()
    m
  }

  /*def destroy(id: Long)(implicit session: DBSession = autoSession): Unit = withSQL {
    update(Question).set(column.deletedAt -> DateTime.now).where.eq(column.id, id)
  }.update.apply()
*/
}