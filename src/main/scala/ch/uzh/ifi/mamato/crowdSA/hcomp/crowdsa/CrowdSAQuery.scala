package ch.uzh.ifi.mamato.crowdSA.hcomp.crowdsa

import ch.uzh.ifi.pdeboer.pplib.hcomp.{HCompAnswer, HCompQuery}

/**
 * Created by mattia on 09.03.15.
 */
case class CrowdSAQuery(query: HCompQuery, properties: CrowdSAQueryProperties) extends Serializable {

  def getQuery(): HCompQuery = {
    query
  }

  def getProperties(): CrowdSAQueryProperties = {
    properties
  }

  def duplicate(answer: HCompAnswer): HCompAnswer ={
    new HCompAnswer {
      override def query: HCompQuery = getQuery()
    }
  }

}
