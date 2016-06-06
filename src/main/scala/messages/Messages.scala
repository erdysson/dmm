package messages

import java.util.Date

import akka.actor.ActorRef
import tasks._

/**
  * Created by valhalla on 06/06/16.
  */

abstract class Message

case class Start(date: Date) extends Message
case class Finish(date: Date) extends Message
case class SendTask(taskAssign: TaskAssign, client: ActorRef) extends Message
case class TaskAssign(name: String, task: Task) extends Message
case class TaskResult(name: String, completedTask: CompletedTask) extends Message
