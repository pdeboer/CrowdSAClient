package ch.uzh.ifi.mamato.crowdPdf.util

import scala.collection.parallel.{ForkJoinTaskSupport, ParSeq}
import scala.concurrent.forkjoin.ForkJoinPool

object CollectionUtils {
	implicit def seqToMPar[T](seq: Seq[T]): MParConverter[T] = new MParConverter[T](seq)
}

class MParConverter[+T](val seq: Seq[T]) {
	def mpar: ParSeq[T] = {
		val parSeq: ParSeq[T] = seq.par
		parSeq.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(300))
		parSeq
	}
}
