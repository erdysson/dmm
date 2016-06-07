package actors

import java.util.Date

import akka.actor._
import akka.routing.RoundRobinGroup
import scala.collection.SortedMap
import scala.collection.mutable.ListBuffer
import matrix.Matrix
import tasks._
import messages._

/**
  * Created by valhalla on 06/06/16.
  */

class Server(name: String, clients: List[ActorRef], system: ActorSystem, taskList: List[Task]) extends Actor {
  private val router = system.actorOf(Props.empty.withRouter(RoundRobinGroup(clients.map(_.path.toString))))
  private val results = ListBuffer.empty[CompletedTask]
  private var startTime: Long = 0

  def receive = {
    case start: Start =>
      startTime = start.millis
      println(s"[$name] : Actor System is being started with ${start.taskList.size} tasks..")
      init()

    case taskResult: TaskResult =>
      println(s"[$name] : task result received from ${taskResult.name} with seq ${taskResult.completedTask.seq} is : ${taskResult.completedTask.result} - ${new Date()}")
      results += taskResult.completedTask
      if (results.size == taskList.size) self ! Finish(System.currentTimeMillis())

    case finish: Finish =>
      println(s"[$name] : Actor System is being terminated... Execution time : ${finish.millis - startTime} ms")
      printResult()
      system.terminate()
  }

  private def printResult(): Unit = {
    val resultMatrix = SortedMap(results.groupBy(_.seqGroup).toSeq:_*).mapValues(cj => cj.sortBy(_.seq).toList.map(c => c.result)).values.toList
    Matrix.print(resultMatrix)
  }

  private def init(): Unit = {
    println(s"[$name] assigning ${taskList.size} task(s) to ${clients.size} client(s)...")
    for (task <- taskList) router ! TaskAssign(name, task)
  }
}

object Server {
  def apply(name: String, system: ActorSystem): Props = Props(classOf[Server], name, system)
}