package transport.udp

import java.util.Date
import akka.actor._
import akka.routing.RoundRobinPool
import scala.concurrent.duration._
import scala.collection.SortedMap

/**
  * Created by taner.gokalp on 13/06/16.
  */

/********** master messages **********/
case class Result(udpPacket: UDPPacket)
case class ReTransmit(dataList: List[Any])
case class Process(taskList: List[Any])
case class Finalize()

/********** worker messages **********/
/* behaviour based messages */
case class WakeUp(now: Date)
case class Sleep(now: Date)

/* data exchange - validation based messages */
case class Send(data: Any)
case class Receive()
case class CheckStatus()

/********** Worker Actor Implementation **********/
class UDPWorker(val channel: UDPChannel, master: ActorRef) extends Actor {

  def active: Receive = {
    case Sleep(now) =>
      println(s"Actor system is deactivated... $now")
      context become inactive

    case Send(data) =>
      channel.send(data)

    case Receive() =>
      val udpPacket = channel.receive(channel.listen())
      udpPacket match {
        case Some(packet) => master ! Result(packet)
      }

    case CheckStatus() =>
      val dataToBeReTransmitted = channel.checkStackStatus()
      if (dataToBeReTransmitted.nonEmpty)
        master ! ReTransmit(dataToBeReTransmitted)
  }

  def inactive: Receive = {
    case WakeUp(now) =>
      println(s"Actor system is activated... $now")
      context become active
  }

  def receive = active
}

object UDPWorker {
  def apply(channel: UDPChannel, master: ActorRef): Props = Props(classOf[UDPWorker], channel, master)
}

/********** Master Actor Implementation **********/
class UDPMaster[A](name: String, channel: UDPChannel, workerPoolSize: Int = Runtime.getRuntime.availableProcessors()) extends Actor {
  // todo : create channel inside
  private val actorSystem = ActorSystem(name)
  private var router: ActorRef = _
  private var scheduler: Cancellable = _
  private var resultMap = SortedMap.empty[Int, A]

  override def preStart(): Unit = {
    router = actorSystem.actorOf(RoundRobinPool(workerPoolSize).props(UDPWorker(channel, self)))
    println("Worker actor system created...")
    scheduler = actorSystem.scheduler.schedule(1.second, 4.seconds, router, CheckStatus())
    println("Status checker scheduler created...")
  }

  def process(taskList: List[Any]): Unit = {
    for (i <- taskList.indices) {
      router ! Send(taskList(i)) // send data
      router ! Receive() // and wait for response
    }
  }

  def active: Receive = {
    case Process(taskList) =>
      process(taskList)

    case Result(udpPacket) =>
      val data = udpPacket.data.asInstanceOf[A]
      resultMap += (data.order -> data.result) // todo : keep groupSeq in task and completed task
      println(s"Master Actor received task result $data")

    case ReTransmit(taskList) =>
      process(taskList)

    case Finalize() =>
      scheduler.cancel()
      // calculate result
      // context become inactive
  }

  def inactive = ???

  def receive = active
}