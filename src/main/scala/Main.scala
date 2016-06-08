import akka.actor.{Props, ActorSystem}
import udp.{Host, Server, Client}

/**
  * Created by valhalla on 06/06/16.
  */

//import akka.actor.{Props, ActorSystem}
//import matrix.Matrix
//import messages.Start
//import actors._
//
object Main {
//  val matrix = Matrix.random(10, withLogging = true)
//  val transposedMatrix = Matrix.transpose(matrix, withLogging = true)
//  val distribution = Matrix.distribute(matrix, transposedMatrix, withLogging = false)
//
//  val system = ActorSystem("Matrix")
//  val clients = List.tabulate(4)(i => system.actorOf(Client(s"Client$i")))
//  val server = system.actorOf(Props(classOf[Server], "Server", clients, system, distribution))
//
//  server ! Start(System.currentTimeMillis(), distribution)

  @throws[Exception]
  def main(args: Array[String]) {
    println("[Chat Window]")

    val server = new Host(port = 9876)
    val clients = List(new Host(port = 9875))

    val actorSystem1 = ActorSystem("Server")
    val serverActor = actorSystem1.actorOf(Props(new Server(server, clients, actorSystem1)), name = "server")

    val actorSystem2 = ActorSystem("Client")
    val clientActor = actorSystem2.actorOf(Props(new Client(clients.head, actorSystem2)), name = "client")

    serverActor ! "start"
    clientActor ! "start"
  }
}