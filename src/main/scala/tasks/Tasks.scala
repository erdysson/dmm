package tasks

import matrix.Matrix

/**
  * Created by valhalla on 06/06/16.
  */

abstract class WorkerTask extends Serializable

@SerialVersionUID(123L)
case class Task(val seq: Int, val seqGroup: Int, val vector1: List[Int], val vector2: List[Int]) extends WorkerTask {
  @transient def complete: CompletedTask = new CompletedTask(seq, seqGroup, Matrix.multiply(vector1, vector2))
  @transient override def toString = s"Task: { seq: $seq, seqGroup: $seqGroup, vector1: $vector1, vector2: $vector2 }"
}

@SerialVersionUID(124L)
case class CompletedTask(val seq: Int, val seqGroup: Int, val result: Int) extends WorkerTask {
  @transient override def toString = s"Completed Task: { seq: $seq, seqGroup: $seqGroup, result: $result }"
}
