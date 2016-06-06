package tasks

import matrix.Matrix

/**
  * Created by valhalla on 06/06/16.
  */

case class Task(val seq: Int, val seqGroup: Int, val vector1: List[Int], val vector2: List[Int]) {
  def complete: CompletedTask = new CompletedTask(seq, seqGroup, Matrix.multiply(vector1, vector2))
  override def toString = s"Task: { seq: $seq, seqGroup: $seqGroup, vector1: $vector1, vector2: $vector2 }"
}

case class CompletedTask(val seq: Int, val seqGroup: Int, val result: Int) {
  override def toString = s"Completed Task: { seq: $seq, seqGroup: $seqGroup, result: $result }"
}
