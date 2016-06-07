/**
  * Created by valhalla on 06/06/16.
  */

import akka.actor.{Props, ActorSystem}
import matrix.Matrix
import messages.Start
import actors._

object Main extends App {
  val matrix = Matrix.random(10, withLogging = true)
  val transposedMatrix = Matrix.transpose(matrix, withLogging = true)
  val distribution = Matrix.distribute(matrix, transposedMatrix, withLogging = false)

  val system = ActorSystem("Matrix")
  val clients = List.tabulate(4)(i => system.actorOf(Client(s"Client$i")))
  val server = system.actorOf(Props(classOf[Server], "Server", clients, system, distribution))

  server ! Start(System.currentTimeMillis(), distribution)

}