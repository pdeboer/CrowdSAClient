package ch.uzh.ifi.mamato.crowdSA.persistence

import ch.uzh.ifi.mamato.crowdSA.model.{StatMethod2Assumption, Assumption, StatMethod}
import scalikejdbc._

/**
 * Created by mattia on 19.03.15.
 */
object StatMethod2AssumptionDAO extends SQLSyntaxSupport[StatMethod2Assumption]{

    override val tableName = "stat_method2assumptions"

    def apply(p: SyntaxProvider[StatMethod2Assumption])(rs: WrappedResultSet): StatMethod2Assumption = apply(p.resultName)(rs)
    def apply(p: ResultName[StatMethod2Assumption])(rs: WrappedResultSet): StatMethod2Assumption = new StatMethod2Assumption(rs.long(p.id), rs.long(p.stat_method_id), rs.long(p.assumption_id))

    val p = StatMethod2AssumptionDAO.syntax("p")

    def find(id: Long)(implicit session: DBSession = autoSession): Option[StatMethod2Assumption] = withSQL {
      select.from(StatMethod2AssumptionDAO as p).where.eq(p.id, id)//.and.append(isNotDeleted)
    }.map(StatMethod2AssumptionDAO(p)).single.apply()

    def findByStatMethodId(id: Long)(implicit session: DBSession = autoSession): List[StatMethod2Assumption] = withSQL {
      select.from(StatMethod2AssumptionDAO as p)
        .where.eq(p.stat_method_id, id)
        .orderBy(p.id)
    }.map(StatMethod2AssumptionDAO(p)).list.apply()

    def findAll()(implicit session: DBSession = autoSession): List[StatMethod2Assumption] = withSQL {
      select.from(StatMethod2AssumptionDAO as p)
        .orderBy(p.id)
    }.map(StatMethod2AssumptionDAO(p)).list.apply()

    def countAll()(implicit session: DBSession = autoSession): Long = withSQL {
      select(sqls.count).from(StatMethod2AssumptionDAO as p)//.where.append(isNotDeleted)
    }.map(rs => rs.long(1)).single.apply().get

    def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[StatMethod2Assumption] = withSQL {
      select.from(StatMethod2AssumptionDAO as p).where.append(sqls"${where}")
        .orderBy(p.id)
    }.map(StatMethod2AssumptionDAO(p)).list.apply()

    def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = withSQL {
      select(sqls.count).from(StatMethod2AssumptionDAO as p).where.append(sqls"${where}")//.and.append(isNotDeleted)
    }.map(_.long(1)).single.apply().get

  }
