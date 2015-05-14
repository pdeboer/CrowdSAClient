package ch.uzh.ifi.mamato.crowdSA.persistence

import ch.uzh.ifi.mamato.crowdSA.model.{Highlight}
import scalikejdbc._

/**
 * Created by mattia on 14.04.15.
 */
object HighlightDAO extends SQLSyntaxSupport[Highlight] {

  override val tableName = "highlights"

  def apply(p: SyntaxProvider[Highlight])(rs: WrappedResultSet): Highlight = apply(p.resultName)(rs)
  def apply(p: ResultName[Highlight])(rs: WrappedResultSet): Highlight = new Highlight(rs.long(p.id),
    rs.string(p.assumption), rs.string(p.terms), rs.string(p.dataset), rs.long(p.remoteQuestionId))

  val p = HighlightDAO.syntax("p")


  def find(id: Long)(implicit session: DBSession = autoSession): Option[Highlight] = withSQL {
    select.from(HighlightDAO as p).where.eq(p.id, id)//.and.append(isNotDeleted)
  }.map(HighlightDAO(p)).single.apply()

  def findByRemoteQuestionId(id: Long)(implicit session: DBSession = autoSession): Option[Highlight] = withSQL {
    select.from(HighlightDAO as p)
      .where.eq(p.remoteQuestionId, id)
      .orderBy(p.id)
  }.map(HighlightDAO(p)).single.apply()

  def findAll()(implicit session: DBSession = autoSession): List[Highlight] = withSQL {
    select.from(HighlightDAO as p)
      .orderBy(p.id)
  }.map(HighlightDAO(p)).list.apply()

  def countAll()(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(HighlightDAO as p)//.where.append(isNotDeleted)
  }.map(rs => rs.long(1)).single.apply().get

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Highlight] = withSQL {
    select.from(HighlightDAO as p).where.append(sqls"${where}")
      .orderBy(p.id)
  }.map(HighlightDAO(p)).list.apply()

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(HighlightDAO as p).where.append(sqls"${where}")//.and.append(isNotDeleted)
  }.map(_.long(1)).single.apply().get

  def create(assumption: String, terms: String, dataset: String, remoteQuestionId: Long)(implicit session: DBSession = autoSession): Highlight = {
    val id = withSQL {
      insert.into(HighlightDAO).namedValues(
        column.assumption -> assumption,
        column.terms -> terms,
        column.dataset -> dataset,
        column.remoteQuestionId -> remoteQuestionId)
    }.updateAndReturnGeneratedKey.apply()
    find(id).get
  }

  def save(id: Long, remoteQId: Long)(implicit session: DBSession = autoSession) = {
    withSQL {
      update(HighlightDAO).set(
        column.remoteQuestionId -> remoteQId
      ).where.eq(column.id, id)//.and.isNull(column.deletedAt)
    }.update.apply()
  }

  /*def destroy(id: Long)(implicit session: DBSession = autoSession): Unit = withSQL {
    update(Paper).set(column.deletedAt -> DateTime.now).where.eq(column.id, id)
  }.update.apply()
*/
}
