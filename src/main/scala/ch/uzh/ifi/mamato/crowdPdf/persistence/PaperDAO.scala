package ch.uzh.ifi.mamato.crowdPdf.persistence

import ch.uzh.ifi.mamato.crowdPdf.model.Paper
import scalikejdbc._

/**
 * Created by Mattia on 19.01.2015.
 */
object PaperDAO extends SQLSyntaxSupport[Paper] {

  override val tableName = "papers"

  def apply(p: SyntaxProvider[Paper])(rs: WrappedResultSet): Paper = apply(p.resultName)(rs)
  def apply(p: ResultName[Paper])(rs: WrappedResultSet): Paper = new Paper(rs.long(p.id), rs.long(p.paper_id))

  val p = PaperDAO.syntax("p")


  def find(id: Long)(implicit session: DBSession = autoSession): Option[Paper] = withSQL {
    select.from(PaperDAO as p).where.eq(p.id, id)//.and.append(isNotDeleted)
  }.map(PaperDAO(p)).single.apply()

  def findAll()(implicit session: DBSession = autoSession): List[Paper] = withSQL {
    select.from(PaperDAO as p)
      //.where.append(isNotDeleted)
      .orderBy(p.id)
  }.map(PaperDAO(p)).list.apply()

  def countAll()(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(PaperDAO as p)//.where.append(isNotDeleted)
  }.map(rs => rs.long(1)).single.apply().get

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Paper] = withSQL {
    select.from(PaperDAO as p).where.append(sqls"${where}")
      //.where.append(isNotDeleted)
      .orderBy(p.id)
  }.map(PaperDAO(p)).list.apply()

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(PaperDAO as p).where.append(sqls"${where}")//.and.append(isNotDeleted)
  }.map(_.long(1)).single.apply().get

  def create(paper_id: Long)(implicit session: DBSession = autoSession): Paper = {
    val id = withSQL {
      insert.into(PaperDAO).namedValues(
        column.paper_id -> paper_id)
    }.updateAndReturnGeneratedKey.apply()
    find(id).get
  }

  def save(m: Paper)(implicit session: DBSession = autoSession): Paper = {
    withSQL {
      update(PaperDAO).set(
        column.paper_id -> m.paper_id
      ).where.eq(column.id, m.id)//.and.isNull(column.deletedAt)
    }.update.apply()
    m
  }

  /*def destroy(id: Long)(implicit session: DBSession = autoSession): Unit = withSQL {
    update(Paper).set(column.deletedAt -> DateTime.now).where.eq(column.id, id)
  }.update.apply()
*/
}