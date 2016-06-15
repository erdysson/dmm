package application.host

import java.net.InetAddress

import akka.actor._
import akka.routing.{RoundRobinRoutingLogic, Router, ActorRefRoutee, RoundRobinPool}
import application.tasks.CompletedTask
import messages.{CheckStatus, Process, Finalize}
import transport.udp.channel.UDPChannel
import transport.udp.worker._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.SortedMap

/**
  * Created by taner.gokalp on 14/06/16.
  */

class Server(name: String, channel: UDPChannel, workerPoolSize: Int = Runtime.getRuntime.availableProcessors()) extends Actor {
  private val actorSystem = ActorSystem(name)
  private var router: ActorRef = _
  private var scheduler: Cancellable = _
  private var resultMap = SortedMap.empty[Int, CompletedTask]

  override def preStart(): Unit = {
    // todo : update router
    router = actorSystem.actorOf(RoundRobinPool(workerPoolSize).props(ServerWorker(channel, (InetAddress.getByName("localhost"), 9875), self)))

//    val router = {
//      val routees = Vector.fill(workerPoolSize) {
//        val r = context.actorOf(ServerWorker(channel, (InetAddress.getLocalHost, 9875), self))
//        context watch r
//        ActorRefRoutee(r)
//      }
//      Router(RoundRobinRoutingLogic(), routees)
//    }

    println(s"[$name] - Worker actor system created...")
    scheduler = actorSystem.scheduler.schedule(1.second, 4.seconds, router, CheckStatus())
    println(s"[$name] - Status checker scheduler created...")
  }

  def process(taskList: List[Any]): Unit = {
    for (i <- taskList.indices) {
      router ! SendToClient(taskList(i)) // send data
      router ! ReceiveFromClient() // and wait for response
    }
  }

  def active: Receive = {
    case Process(taskList) =>
      process(taskList)

    case ResultFromClient(udpPacket) =>
      val data = udpPacket.data.asInstanceOf[CompletedTask]
      resultMap += (data.order -> data)
      println(s"[$name] Master actor received task result $data")

    case ReTransmitToClient(taskList) =>
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
  def apply(name: String, channel: UDPChannel, workerPoolSize: Int = Runtime.getRuntime.availableProcessors()): Props = Props(classOf[Server], name, channel, workerPoolSize)
}