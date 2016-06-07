package messages

import tasks._

/**
  * Created by valhalla on 06/06/16.
  */

abstract class Message

case class Start(millis: Long, taskList: List[Task]) extends Message
case class Finish(millis: Long) extends Message
case class TaskAssign(name: String, task: Task) extends Message
case class TaskResult(name: String, completedTask: CompletedTask) extends Message
