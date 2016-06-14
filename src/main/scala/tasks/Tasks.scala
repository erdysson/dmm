package tasks

import matrix.Matrix

/**
  * Created by taner.gokalp on 06/06/16.
  */

case class Task(val order: Int, val vector1: Vector[Int], val vector2: Vector[Int]) {
  def complete(task: Task): CompletedTask = new CompletedTask(task.order, Matrix.multiply(task.vector1, task.vector2))
}

case class CompletedTask(val order: Int, val result: Int)