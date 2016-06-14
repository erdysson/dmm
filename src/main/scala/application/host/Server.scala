package application.host

import akka.actor._
import akka.routing.RoundRobinPool
import messages._
import transport.udp.channel.UDPChannel
import transport.udp.worker.UDPWorker
import scala.concurrent.duration._
import scala.collection.SortedMap

/**
  * Created by taner.gokalp on 14/06/16.
  */
class Server[A](name: String, channel: UDPChannel, workerPoolSize: Int = Runtime.getRuntime.availableProcessors()) extends Actor {
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
      resultMap += (data.order -> data.result)
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

object Server {
  def apply[A](name: String, channel: UDPChannel, workerPoolSize: Int = Runtime.getRuntime.availableProcessors()): Props = Props(classOf[Server[A]], name, channel, workerPoolSize)
}