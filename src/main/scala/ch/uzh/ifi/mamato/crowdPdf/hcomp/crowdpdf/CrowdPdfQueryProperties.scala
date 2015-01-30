package ch.uzh.ifi.mamato.crowdPdf.hcomp.crowdpdf

import ch.uzh.ifi.mamato.crowdPdf.model.Highlight
import ch.uzh.ifi.pdeboer.pplib.hcomp.HCompQueryProperties

/**
 * Created by Mattia on 30.01.2015.
 */
class CrowdPdfQueryProperties(val paperId: Long, val questionType: String, val highlight: Highlight, val reward_cents: Int) extends HCompQueryProperties
