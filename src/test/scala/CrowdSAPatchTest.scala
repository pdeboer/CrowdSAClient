import ch.uzh.ifi.mamato.crowdSA.process.entities.CrowdSAPatch
import ch.uzh.ifi.mamato.crowdSA.util.PdfUtils
import org.junit.{Assert, Test}

import scala.collection.mutable

/**
 * Created by mattia on 08.06.15.
 */
class CrowdSAPatchTest {

  @Test
  def testCrowdSAPatch(): Unit = {
    val patch = new CrowdSAPatch("How old are you?", "Discovery", "age", 1, "PositiveAge")

    val patch1 = patch.duplicate("")

    Assert.assertEquals(patch, patch1)
    Assert.assertTrue(patch.question.equals(patch1.question))
  }
}
