package actors

import java.util.Date

import akka.actor.{ActorLogging, Props, ActorSystem, Actor}
import tasks._
import messages._

import scala.collection.mutable.ListBuffer

/**
  * Created by valhalla on 06/06/16.
  */

class Server(name: String, system: ActorSystem) extends Actor with ActorLogging {
  val dummyTasks = List.tabulate(4)(index => new Task(seq = index, seqGroup = index * 2, vector1 = List(index * 1, index * 2), vector2 = List(index * 3, index * 4)))

  val results = ListBuffer.empty[CompletedTask]

  def receive = {
    case start: Start =>
      println(s"[$name] : Actor System is being started at ${start.date}")

    case sendTask: SendTask =>
      sendTask.client ! sendTask.taskAssign

    case taskResult: TaskResult =>
      println(s"[$name] : task result received from ${taskResult.name} is : ${taskResult.completedTask.result} - ${new Date()}")
      results += taskResult.completedTask
      if (results.size == 4) self ! Finish(new Date())

    case finish: Finish =>
      println(s"Results : ${results.toList}")
      println(s"[$name] : Actor System is being terminated at ${finish.date}")
      system.terminate()
  }

  def init(): Unit = {


  }
}

object Server {
  def apply(name: String, system: ActorSystem): Props = Props(classOf[Server], name, system)
}