package matrix

/**
  * Created by valhalla on 06/06/16.
  */

import scala.collection.mutable.ListBuffer
import tasks.Task

object Matrix {
  def print(matrix: Vector[Vector[Int]]) {
    for (i <- matrix.indices) {
      println(s"| ${matrix(i).mkString(" ")} |")
    }
  }

  def transpose(matrix: Vector[Vector[Int]], withLogging: Boolean = false) = {
    val transposeStart = System.currentTimeMillis()
    val transposedMatrix: Vector[Vector[Int]] = Vector.tabulate(matrix.size)(rowIndex => Vector.tabulate(matrix.size)(columnIndex => matrix(columnIndex)(rowIndex)))
    val transposeEnd = System.currentTimeMillis()

    if (withLogging) {
      println(s"TRANSPOSITION TIME : ${transposeEnd - transposeStart} milliseconds for ${matrix.size} x ${matrix.size} matrix")
      println(s"<< transposed ${matrix.size} x ${matrix.size} matrix >>")
      Matrix.print(transposedMatrix)
      println("......................................................................................")
    }
    transposedMatrix
  }

  def multiply(vector1: Vector[Int], vector2: Vector[Int], withLogging: Boolean = false): Int = {
    if (vector1.isEmpty || vector2.isEmpty)
      0
    else
      vector1.head * vector2.head + multiply(vector1.tail, vector2.tail)
  }

  def random(size: Int, withLogging: Boolean = false): Vector[Vector[Int]] = {
    val randomMatrixStart = System.currentTimeMillis()
    val matrix: Vector[Vector[Int]] = Vector.fill(size, size)((Math.random() * 5 + 1).toInt)
    val randomMatrixEnd = System.currentTimeMillis()

    if (withLogging) {
      println(s"RANDOM MATRIX CREATION TIME : ${randomMatrixEnd - randomMatrixStart} milliseconds for ${matrix.size} x ${matrix.size} matrix")
      println(s"<< generated ${matrix.size} x ${matrix.size} matrix >>")
      Matrix.print(matrix)
      println("......................................................................................")
    }
    matrix
  }

  // todo : recurse or improve the distribution
  def distribute(matrix1: Vector[Vector[Int]], matrix2: Vector[Vector[Int]], withLogging: Boolean = false): ListBuffer[Task] = {
    var order = 0
    var distribution = ListBuffer.empty[Task]

    val distributionStart = System.currentTimeMillis()

    matrix1 foreach(m1Row => {
      matrix2 foreach(m2Row => {
        order += 1
        val task = new Task(order, m1Row, m2Row)
        distribution += task
      })
    })

    if (withLogging) {
      val distributionEnd = System.currentTimeMillis()
      println(s"DISTRIBUTION TIME : ${distributionEnd - distributionStart} milliseconds for ${matrix1.size} x ${matrix2.size} matrix multiplication")
      println(s"<< generated distribution list with ${distribution.size} operation(s) : $distribution >>")
      println("......................................................................................")
    }
    distribution
  }
}