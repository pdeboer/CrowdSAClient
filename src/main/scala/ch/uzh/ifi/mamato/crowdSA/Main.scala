package ch.uzh.ifi.mamato.crowdSA

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.{CrowdSAQueryProperties, CrowdSAPortalAdapter}
import ch.uzh.ifi.mamato.crowdSA.model.Highlight
import ch.uzh.ifi.mamato.crowdSA.persistence.DBSettings
import ch.uzh.ifi.mamato.crowdSA.util.LazyLogger
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQuery, HComp}


/**
 * Created by Mattia on 18.12.2014.
 */


object Main extends App with LazyLogger {
  logger.info("**** Mattia Amato CrowdPdf Client ****")
  logger.info("Initializing database...")

  DBSettings.initialize()

  val crowdSA = new CrowdSAPortalAdapter()
  HComp.addPortal(crowdSA)

  val query = new HCompQuery {
    override def question: String = "How old are you?"

    override def title: String = "Info about you"

    override def suggestedPaymentCents: Int = 10
  }

  val createPaperAndProcessQuery = new Thread(new Runnable {
    def run(): Unit = {
      //Upload paper
      val remote_id = crowdSA.service.uploadPaper("src/main/resources/169-G652.pdf", "Cancer Classification of Bioinformatics data using ANOVA", 50, true)
      if(remote_id > 0){
        val highlight = new Highlight("Normality", "ANOVA,statistic,on the")
        val properties = new CrowdSAQueryProperties(remote_id, "Boolean", highlight, 10, 60*60*24*365, 5)
        //Process query
        crowdSA.processQuery(query, properties)
      } else {
        logger.error("Error while uploading the paper")
      }
    }
  })

  val cancelQuery = new Thread(new Runnable {
    def run(): Unit = {
      logger.debug("Try to cancel query")
      crowdSA.cancelQuery(query)
    }
  })

  createPaperAndProcessQuery.start()

  //Thread.sleep(5000)

  //b.start()
}