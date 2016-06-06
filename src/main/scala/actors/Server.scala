package actors

import java.util.Date

import akka.actor._
import akka.routing.RoundRobinGroup
import tasks._
import messages._

import scala.collection.mutable.ListBuffer

/**
  * Created by valhalla on 06/06/16.
  */

class Server(name: String, clients: List[ActorRef], system: ActorSystem) extends Actor with ActorLogging {
  val router = system.actorOf(Props.empty.withRouter(RoundRobinGroup(clients.map(_.path.toString))))
  val taskSize = 1000000
  val dummyTasks = List.tabulate(taskSize)(index => new Task(seq = index,
                                                             seqGroup = index + 1,
                                                             vector1 = List(dummyRandomIntGenerator, dummyRandomIntGenerator),
                                                             vector2 = List(dummyRandomIntGenerator, dummyRandomIntGenerator)))

  val results = ListBuffer.empty[CompletedTask]

  def receive = {
    case start: Start =>
      println(s"[$name] : Actor System is being started at ${start.date}")
      init()

    case taskResult: TaskResult =>
      println(s"[$name] : task result received from ${taskResult.name} with seq ${taskResult.completedTask.seq} is : ${taskResult.completedTask.result} - ${new Date()}")
      results += taskResult.completedTask
      if (results.size == taskSize) self ! Finish(new Date())

    case finish: Finish =>
      // println(s"Results : ${results.sortBy(_.seq).toList}")
      println(s"[$name] : Actor System is being terminated at ${finish.date}")
      // todo : may poison pill required for router ?
      system.terminate()
  }

  def dummyRandomIntGenerator: Int = (Math.random() * 10 + 1).toInt

  def init(): Unit = {
    println(s"[$name] assigning $taskSize task(s) to ${clients.size} client(s)...")
    for (task <- dummyTasks) router ! TaskAssign(name, task)
  }
}

object Server {
  def apply(name: String, system: ActorSystem): Props = Props(classOf[Server], name, system)
}