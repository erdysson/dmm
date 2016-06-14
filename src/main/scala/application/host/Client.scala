package application.host

import akka.actor._
import akka.routing.RoundRobinPool
import messages._
import transport.udp.channel.UDPChannel
import transport.udp.worker.UDPWorker
import scala.concurrent.duration._

/**
  * Created by taner.gokalp on 14/06/16.
  */

case class Listen()

class Client[A](name: String, channel: UDPChannel, workerPoolSize: Int = Runtime.getRuntime.availableProcessors()) extends Actor {
  private val actorSystem = ActorSystem(name)
  private var router: ActorRef = _
  private var scheduler: Cancellable = _

  override def preStart(): Unit = {
    router = actorSystem.actorOf(RoundRobinPool(workerPoolSize).props(UDPWorker(channel, self)))
    println(s"[$name] - Worker actor system created...")
    scheduler = actorSystem.scheduler.schedule(1.second, 4.seconds, router, CheckStatus())
    println(s"[$name] - Status checker scheduler created...")
  }

  def process(resultList: List[Any]): Unit = {
    for (i <- resultList.indices) {
      router ! Send(resultList(i).complete)
      router ! Receive()
    }
  }

  def active: Receive = {
    // todo : continuous listening
    case Listen() =>
      router ! Receive()

    case Result(udpPacket) =>
      val data = udpPacket.data.asInstanceOf[A]
      println(s"[$name] Master actor received task result $data")
      process(List(data))

    case ReTransmit(resultList) =>
      process(resultList)

    case Finalize() =>
      scheduler.cancel()
      // context become inactive
  }

  def inactive = ???

  def receive = active

}

object Client {
  def apply[A](name: String, channel: UDPChannel, workerPoolSize: Int = Runtime.getRuntime.availableProcessors()): Props = Props(classOf[Client[A]], name, channel, workerPoolSize)
}
