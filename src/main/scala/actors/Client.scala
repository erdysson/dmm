package actors

import java.util.Date

import akka.actor.{Props, Actor}
import messages._

/**
  * Created by valhalla on 06/06/16.
  */

class Client(name: String) extends Actor {
  def receive = {
    case taskAssign: TaskAssign =>
      println(s"[$name] : Task received from ${taskAssign.name} with seq number ${taskAssign.task.seq} - ${new Date()}")
      sender ! TaskResult(name, taskAssign.task.complete)
  }
}

object Client {
  def apply(name: String): Props = Props(classOf[Client], name)
}
