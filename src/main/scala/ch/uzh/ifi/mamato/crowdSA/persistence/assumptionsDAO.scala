package ch.uzh.ifi.mamato.crowdSA.persistence

import ch.uzh.ifi.mamato.crowdSA.model.Assumption
import scalikejdbc._

/**
 * Created by mattia on 19.03.15.
 */
object AssumptionsDAO extends SQLSyntaxSupport[Assumption] {
  override val tableName = "assumptions"

  def apply(p: SyntaxProvider[Assumption])(rs: WrappedResultSet): Assumption = apply(p.resultName)(rs)
  def apply(p: ResultName[Assumption])(rs: WrappedResultSet): Assumption = new Assumption(rs.long(p.id), rs.string(p.assumption))

  val p = AssumptionsDAO.syntax("p")


  def find(id: Long)(implicit session: DBSession = autoSession): Option[Assumption] = withSQL {
    select.from(AssumptionsDAO as p).where.eq(p.id, id)//.and.append(isNotDeleted)
  }.map(AssumptionsDAO(p)).single.apply()

  def findByAssumption(assumption: String)(implicit session: DBSession = autoSession): Option[Assumption] = withSQL {
    select.from(AssumptionsDAO as p)
      .where.eq(p.assumption, assumption)
      .orderBy(p.id)
  }.map(AssumptionsDAO(p)).single.apply()

  def findAll()(implicit session: DBSession = autoSession): List[Assumption] = withSQL {
    select.from(AssumptionsDAO as p)
      .orderBy(p.id)
  }.map(AssumptionsDAO(p)).list.apply()

  def countAll()(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(AssumptionsDAO as p)//.where.append(isNotDeleted)
  }.map(rs => rs.long(1)).single.apply().get

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Assumption] = withSQL {
    select.from(AssumptionsDAO as p).where.append(sqls"${where}")
      .orderBy(p.id)
  }.map(AssumptionsDAO(p)).list.apply()

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(AssumptionsDAO as p).where.append(sqls"${where}")//.and.append(isNotDeleted)
  }.map(_.long(1)).single.apply().get

  def create(assumption: String)(implicit session: DBSession = autoSession): Assumption = {
    val id = withSQL {
      insert.into(AssumptionsDAO).namedValues(
        column.assumption -> assumption)
    }.updateAndReturnGeneratedKey.apply()
    find(id).get
  }

  def save(m: Assumption)(implicit session: DBSession = autoSession): Assumption = {
    withSQL {
      update(AssumptionsDAO).set(
        column.assumption -> m.assumption
      ).where.eq(column.id, m.id)//.and.isNull(column.deletedAt)
    }.update.apply()
    m
  }

}
