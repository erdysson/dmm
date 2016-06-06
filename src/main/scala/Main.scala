/**
  * Created by valhalla on 06/06/16.
  */

import akka.actor.{ActorPath, ActorRef, Props, ActorSystem}
import akka.routing.RoundRobinGroup

import messages.{SendTask, TaskAssign}
import actors._
import tasks.Task

object Main extends App {

  val dummyTasks = List.tabulate(4)(index => new Task(seq = index, seqGroup = index * 2, vector1 = List(index * 1, index * 2), vector2 = List(index * 3, index * 4)))

  val system = ActorSystem("MatrixMultiplication")

  val client1 = system.actorOf(Client("Client 1"))
  val client2 = system.actorOf(Client("Client 2"))

  println(s"c1 ${client1.path.toString}")

  val routees = List(client1.path.toString, client2.path.toString)
  val server = system.actorOf(Server("Server", system)) // .withRouter(RoundRobinGroup(routees)))

  server ! SendTask(TaskAssign("Server", dummyTasks(0)), client1)
  server ! SendTask(TaskAssign("Server", dummyTasks(1)), client2)
  server ! SendTask(TaskAssign("Server", dummyTasks(2)), client1)
  server ! SendTask(TaskAssign("Server", dummyTasks(3)), client2)

}