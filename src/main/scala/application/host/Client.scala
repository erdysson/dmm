package application.host

import java.net.InetAddress

import akka.actor._
import akka.routing.RoundRobinPool
import application.tasks.Task
import messages.{CheckStatus, Listen, Finalize}

import transport.udp.channel.UDPChannel
import transport.udp.worker._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by taner.gokalp on 14/06/16.
  */

class Client(name: String, channel: UDPChannel, workerPoolSize: Int = Runtime.getRuntime.availableProcessors()) extends Actor {
  private val actorSystem = ActorSystem(name)
  private var router: ActorRef = _
  private var scheduler: Cancellable = _

  override def preStart(): Unit = {
    router = actorSystem.actorOf(RoundRobinPool(workerPoolSize).props(ClientWorker(channel, self)))
    println(s"[$name] - Worker actor system created...")
    scheduler = actorSystem.scheduler.schedule(1.second, 4.seconds, router, CheckStatus())
    println(s"[$name] - Status checker scheduler created...")
  }

  def process(configuredResultList: List[(Any, (InetAddress, Int))]): Unit = {
    for (i <- configuredResultList.indices) {
      val oneResult = configuredResultList(i)
      router ! SendToServer(oneResult._1.asInstanceOf[Task].complete(), oneResult._2)
      router ! ReceiveFromServer()
    }
  }

  def active: Receive = {
    // todo : continuous listening
    case Listen() =>
      router ! ReceiveFromServer()

    case ResultFromServer(udpPacket, remoteConfig) =>
      val data = udpPacket.data
      println(s"[$name] Master actor received task result $data")
      process(List((data, remoteConfig)))

    case ReTransmitToServer(configuredReTransmissionList) =>
      process(configuredReTransmissionList)

    case Finalize() =>
      scheduler.cancel()
      // context become inactive
  }

  def inactive = ???

  def receive = active

}

object Client {
  def apply[A](name: String, channel: UDPChannel, workerPoolSize: Int = Runtime.getRuntime.availableProcessors()): Props = Props(classOf[Client], name, channel, workerPoolSize)
}
