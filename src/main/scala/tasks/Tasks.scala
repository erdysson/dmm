package tasks

import matrix.Matrix

/**
  * Created by taner.gokalp on 06/06/16.
  */

// todo : move complete task into actor

abstract class WorkerTask

case class Task(val order: Int, val vector1: Vector[Int], val vector2: Vector[Int]) extends WorkerTask

case class CompletedTask(val order: Int, val result: Int) extends WorkerTask

object Task {
  def complete(task: Task): CompletedTask = new CompletedTask(task.order, Matrix.multiply(task.vector1, task.vector2))
}