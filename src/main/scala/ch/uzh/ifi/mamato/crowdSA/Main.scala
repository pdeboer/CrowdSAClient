package ch.uzh.ifi.mamato.crowdSA

import ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa.CrowdSAPortalAdapter
import ch.uzh.ifi.mamato.crowdSA.persistence._
import ch.uzh.ifi.mamato.crowdSA.process.entities.CrowdSAPatch
import ch.uzh.ifi.mamato.crowdSA.process.{ExtractStatisticsProcess, ExtractStatisticsRecombination}
import ch.uzh.ifi.mamato.crowdSA.util.{LazyLogger, PdfUtils}
import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp

import scala.collection.mutable

/**
 * Created by Mattia on 18.12.2014.
 */

object Main extends App with LazyLogger {
  logger.info("**** Mattia Amato - CrowdSA Client ****")
  if(args.length != 2){
    logger.info("Please use two arguments when starting the client. " +
      "The first is the path to the PDF file, the second the title of the PDF.")
    System.exit(0)
  }

    logger.info("Initializing database...")

    DBSettings.initialize()

    logger.debug("Creating portal adapter...")
    val crowdSA = new CrowdSAPortalAdapter()
    HComp.addPortal(crowdSA)
    logger.debug("Portal added")

    val pathPdf = args(0)
    val titlePdf = args(1)

    // create list of possible matches
    val toMatch = StatMethodsDAO.findAll()
    logger.debug("Found " + toMatch.length + " statistical methods in the database")

    var statMethod2ContextStatMethod = new mutable.MutableList[(String, String)]
    try {
      // open pdf and convert it to text
      val pdfToText = PdfUtils.getTextFromPdf(pathPdf).get

      // get context of statistical methods that correspond to the ones present in the database
      statMethod2ContextStatMethod = PdfUtils.findContextMatch(pdfToText, toMatch.map(_.stat_method).toList)
      logger.debug("Found " + statMethod2ContextStatMethod.length + " matches in the paper")
    } catch {
      case e: Exception => e.printStackTrace()
    }

    // upload paper
    val remote_id = CrowdSAPortalAdapter.service.uploadPaper(pathPdf, titlePdf, 15000, true)
    if(remote_id < 0){
      logger.error("Error while uploading the paper")
    }
    else {
      val discoveryQuestions = new mutable.MutableList[CrowdSAPatch]

      val allMethods = new mutable.HashMap[String, mutable.MutableList[String]]

      // create DISCOVERY questions for each statistical method that matched
      statMethod2ContextStatMethod.foreach {
        m =>

          if(!allMethods.get(m._1).isDefined){
            allMethods += m._1 -> new mutable.MutableList[String]
          }

          allMethods.get(m._1).get += m._2

          logger.debug("Creating DISCOVERY question for match: " + m._1)
          // Add to the data structure: the question as well as the properties
          discoveryQuestions += new CrowdSAPatch("Method: <u><i> " + m._1 + " </i></u>", "Discovery",
            "[\""+m._2+"\"]", remote_id, "Discovery")
      }

      // create MISSING question
      //val findMissingMethods = new CrowdSAPatch("Please find all the methods which are not highlighted on the paper", "Missing", "[]", remote_id, "Missing", allMethods)
      //discoveryQuestions += findMissingMethods

      val extractStatisticsProcess = new ExtractStatisticsProcess(crowdSA, discoveryQuestions.toList)

      val DEFAULT_BUDGET: Int = 15000
      logger.debug("Setting budget...")
      crowdSA.setBudget(Some(DEFAULT_BUDGET))

      // We have only 1 recombination, we don't need to create all the possible processes variants
      val results = (ExtractStatisticsRecombination.recombinations.head, extractStatisticsProcess.run(ExtractStatisticsRecombination.recombinations.head))

      println("finished evaluation.")

      println("**** Result: "+results._2)

      println("all done")

    }

}