package matrix

/**
  * Created by valhalla on 06/06/16.
  */

import scala.collection.mutable.ListBuffer
import tasks.Task

object Matrix {
  def print(matrix: List[List[Int]]) {
    for (i <- matrix.indices) {
      println(s"| ${matrix(i).mkString(" ")} |")
    }
  }

  // todo : use Vector or ListBuffer
  def transpose(matrix: List[List[Int]], withLogging: Boolean = false) = {
    val transposeStart = System.currentTimeMillis()
    val transposedMatrix: List[List[Int]] = List.tabulate(matrix.size)(rowIndex => List.tabulate(matrix.size)(columnIndex => matrix(columnIndex)(rowIndex)))
    val transposeEnd = System.currentTimeMillis()

    if (withLogging) {
      println(s"TRANSPOSITION TIME : ${transposeEnd - transposeStart} milliseconds for ${matrix.size} x ${matrix.size} matrix")
      println(s"<< transposed ${matrix.size} x ${matrix.size} matrix >>")
      Matrix.print(transposedMatrix)
      println("......................................................................................")
    }

    transposedMatrix
  }

  // todo : convert to Vector
  def multiply(vector1: List[Int], vector2: List[Int], withLogging: Boolean = false): Int = {
    if (vector1.isEmpty || vector2.isEmpty)
      0
    else
      vector1.head * vector2.head + multiply(vector1.tail, vector2.tail)
  }

  // todo : use Vector
  def random(size: Int, withLogging: Boolean = false): List[List[Int]] = {
    val randomMatrixStart = System.currentTimeMillis()
    val matrix: List[List[Int]] = List.fill(size)(List.fill(size)((Math.random() * 5 + 1).toInt))
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
  def distribute(matrix1: List[List[Int]], matrix2: List[List[Int]], withLogging: Boolean = false): List[Task] = {
    var seq = 0
    var seqGroup = 0
    var schedule = ListBuffer.empty[Task]

    val distributionStart = System.currentTimeMillis()

    matrix1 foreach(m1Row => {
      seqGroup += 1
      matrix2 foreach(m2Row => {
        seq += 1
        val task = new Task(seq, seqGroup, m1Row, m2Row)
        schedule += task
      })
    })
    val distribution = schedule.toList

    if (withLogging) {
      val distributionEnd = System.currentTimeMillis()
      println(s"DISTRIBUTION TIME : ${distributionEnd - distributionStart} milliseconds for ${matrix1.size} x ${matrix2.size} matrix multiplication")
      println(s"<< generated distribution list with ${distribution.size} operation(s) : $distribution >>")
      println("......................................................................................")
    }

    distribution
  }
}