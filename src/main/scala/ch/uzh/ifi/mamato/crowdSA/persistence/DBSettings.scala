package ch.uzh.ifi.mamato.crowdSA.persistence

/**
 * Created by Mattia on 19.01.2015.
 */

import scalikejdbc._
import scalikejdbc.config._

trait DBSettings {
  DBSettings.initialize()
}

object DBSettings {

  private var isInitialized = false

  def initialize(): Unit = this.synchronized {
    if (isInitialized) return
    DBs.setupAll()

    GlobalSettings.loggingSQLErrors = true
    //GlobalSettings.sqlFormatter = SQLFormatterSettings("devteam.misc.HibernateSQLFormatter")
    DBInitializer.run()
    isInitialized = true
  }

}
