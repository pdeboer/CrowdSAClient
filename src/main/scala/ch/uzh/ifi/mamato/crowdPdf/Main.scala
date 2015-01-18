package ch.uzh.ifi.mamato.crowdPdf


import ch.uzh.ifi.mamato.crowdPdf.hcomp.crowdpdf.CrowdPdfPortalAdapter
import ch.uzh.ifi.mamato.crowdPdf.util.LazyLogger
import ch.uzh.ifi.pdeboer.pplib.hcomp.HComp


/**
 * Created by Mattia on 18.12.2014.
 */

object Main extends App with LazyLogger {

  //def main(args: Array[String]) = {
    logger.info("**** Mattia Amato CrowdPdf Client ****")
    if(args.length != 3){
      println("""
        Usage: ./crowdPdf.sh paperPath paperTitle budget

        All the parameters are needed and have to be written
        in this specific order.
      """)
      System.exit(-1)
    }
    val crowdPdf = new CrowdPdfPortalAdapter("MattiaTest", sandbox = false)
    HComp.addPortal(crowdPdf)
    //val pdfByteArray = pdfToByteArray(args(0))

    val pManager = PapersManager.createPaperRequest(args(0), args(1), new Integer(args(2)))

  //}

  /*def pdfToByteArray(path: String): Array[Byte] ={

    val p = Paths.get(path)
    val data = Files.readAllBytes(p)

    data
  }*/

}