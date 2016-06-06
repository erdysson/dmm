/**
  * Created by valhalla on 06/06/16.
  */

import java.util.Date

import akka.actor.{Props, ActorSystem}
import messages.Start
import actors._

object Main extends App {
  val system = ActorSystem("MatrixMultiplication")
  val clients = List.tabulate(5)(i => system.actorOf(Client(s"Client $i")))
  val server = system.actorOf(Props(classOf[Server], "Server", clients, system))

  server ! Start(new Date())

}