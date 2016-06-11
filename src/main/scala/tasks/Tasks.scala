package tasks

import matrix.Matrix

/**
  * Created by taner.gokalp on 06/06/16.
  */

// todo : move complete task into actor

abstract class WorkerTask

case class Task(val seq: Int, val seqGroup: Int, val vector1: List[Int], val vector2: List[Int]) extends WorkerTask
case class CompletedTask(val seq: Int, val seqGroup: Int, val result: Int) extends WorkerTask

object Task {
  def complete(task: Task): CompletedTask = new CompletedTask(task.seq, task.seqGroup, Matrix.multiply(task.vector1, task.vector2))
}