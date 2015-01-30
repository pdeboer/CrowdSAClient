package ch.uzh.ifi.mamato.crowdPdf


import ch.uzh.ifi.mamato.crowdPdf.hcomp.crowdpdf.{CrowdPdfQueryProperties, CrowdPdfPortalAdapter}
import ch.uzh.ifi.mamato.crowdPdf.model.Highlight
import ch.uzh.ifi.mamato.crowdPdf.persistence.DBSettings
import ch.uzh.ifi.mamato.crowdPdf.util.LazyLogger
import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompQueryProperties, HCompQuery, HComp}


/**
 * Created by Mattia on 18.12.2014.
 */



object Main extends App with LazyLogger {
  logger.info("**** Mattia Amato CrowdPdf Client ****")
  logger.info("Initializing database...")

  DBSettings.initialize()

  if(args.length != 3){
    println("""
        Usage: ./crowdPdf.sh paperPath paperTitle budget

        All the parameters are needed and have to be written
        in this specific order.
            """)
    System.exit(-1)
  }

  val crowdPdf = new CrowdPdfPortalAdapter()
  HComp.addPortal(crowdPdf)

  val query = new HCompQuery {
    override def question: String = "Awesome Question question?"

    override def title: String = "Awesome Question Title"

    override def suggestedPaymentCents: Int = 1
  }

  val a = new Thread(new Runnable {
    def run(): Unit = {
      val highlight = new Highlight("Normality", "ANOVA,MANOVA,on the")
      val properties = new CrowdPdfQueryProperties(2, "Boolean", highlight, 10)
      crowdPdf.processQuery(query, properties)
    }
  })

  val b = new Thread(new Runnable {
    def run(): Unit = {
      logger.debug("Try to cancel query")
      crowdPdf.cancelQuery(query)
    }
  })

  a.start()

  //Thread.sleep(5000)

  //b.start()
}