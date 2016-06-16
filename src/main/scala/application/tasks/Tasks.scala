package application.tasks

import application.matrix.Matrix

/**
  * Created by taner.gokalp on 06/06/16.
  */

case class Task(val order: Int, val groupOrder: Int, val vector1: Vector[Int], val vector2: Vector[Int]) {
  def complete(): CompletedTask = new CompletedTask(order, groupOrder, Matrix.multiply(vector1, vector2))
}

case class CompletedTask(val order: Int, val groupOrder: Int, val result: Int)