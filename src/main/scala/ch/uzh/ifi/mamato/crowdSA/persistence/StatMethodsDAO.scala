package ch.uzh.ifi.mamato.crowdSA.persistence

import ch.uzh.ifi.mamato.crowdSA.model.StatMethod
import scalikejdbc._

/**
 * Created by Mattia on 19.01.2015.
 */
object StatMethodsDAO extends SQLSyntaxSupport[StatMethod] {

  override val tableName = "stat_methods"

  def apply(p: SyntaxProvider[StatMethod])(rs: WrappedResultSet): StatMethod = apply(p.resultName)(rs)
  def apply(p: ResultName[StatMethod])(rs: WrappedResultSet): StatMethod = new StatMethod(rs.long(p.id), rs.string(p.stat_method))

  val p = StatMethodsDAO.syntax("p")


  def find(id: Long)(implicit session: DBSession = autoSession): Option[StatMethod] = withSQL {
    select.from(StatMethodsDAO as p).where.eq(p.id, id)//.and.append(isNotDeleted)
  }.map(StatMethodsDAO(p)).single.apply()

  def findByStatMethod(stat_method: String)(implicit session: DBSession = autoSession): Option[StatMethod] = withSQL {
    select.from(StatMethodsDAO as p)
      .where.eq(p.stat_method, stat_method)
      .orderBy(p.id)
  }.map(StatMethodsDAO(p)).single.apply()

  def findAll()(implicit session: DBSession = autoSession): List[StatMethod] = withSQL {
    select.from(StatMethodsDAO as p)
      .orderBy(p.id)
  }.map(StatMethodsDAO(p)).list.apply()

  def countAll()(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(StatMethodsDAO as p)//.where.append(isNotDeleted)
  }.map(rs => rs.long(1)).single.apply().get

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[StatMethod] = withSQL {
    select.from(StatMethodsDAO as p).where.append(sqls"${where}")
      .orderBy(p.id)
  }.map(StatMethodsDAO(p)).list.apply()

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = withSQL {
    select(sqls.count).from(StatMethodsDAO as p).where.append(sqls"${where}")//.and.append(isNotDeleted)
  }.map(_.long(1)).single.apply().get

  def create(stat_method: String)(implicit session: DBSession = autoSession): StatMethod = {
    val id = withSQL {
      insert.into(StatMethodsDAO).namedValues(
        column.stat_method -> stat_method)
    }.updateAndReturnGeneratedKey.apply()
    find(id).get
  }

  def save(m: StatMethod)(implicit session: DBSession = autoSession): StatMethod = {
    withSQL {
      update(StatMethodsDAO).set(
        column.stat_method -> m.stat_method
      ).where.eq(column.id, m.id)//.and.isNull(column.deletedAt)
    }.update.apply()
    m
  }

  /*def destroy(id: Long)(implicit session: DBSession = autoSession): Unit = withSQL {
    update(Paper).set(column.deletedAt -> DateTime.now).where.eq(column.id, id)
  }.update.apply()
*/
}