package demo

import akka.actor.{Props, ActorRef, ActorSystem, Actor}
import akka.routing.RoundRobinPool
import application.tasks.Task
import transport.rudp.{RUDPData, UDPChannel}
import scala.concurrent.duration._

/**
  * Created by taner.gokalp on 14/06/16.
  */

case class CalculateAndSend(rudpData: RUDPData)
case class ReTransmit(retransmissionList: List[RUDPData])

class StatusChecker(channel: UDPChannel, router: ActorRef) extends Actor {
  def checkStatus(): Unit = {
    val packetsToBeReTransmitted = channel.checkStatus()
    packetsToBeReTransmitted match {
      case Some(retransmissionList) =>
        println(s"[Status Checker] : packets to retransmit : $retransmissionList")
        router ! ReTransmit(retransmissionList)
      case None =>
        println("[Status Checker] : There is no packet to re transmit")
    }
  }

  def active: Receive = {
    case "check" =>
      checkStatus()

    case "sleep" =>
      context become inactive
  }

  def inactive: Receive = {
    case "wake up" =>
      context become active
  }

  def receive = active
}

class Calculator(channel: UDPChannel) extends Actor {
  def receive = {
    case CalculateAndSend(rudpData) =>
      // println("calculator actor calculating...")
      channel.send(rudpData.data.asInstanceOf[Task].complete(), rudpData.receiverAddress, rudpData.receiverPort)

    case ReTransmit(retransmissionList) =>
      // println(s"Calculator Actor retransmitting ${retransmissionList.length} data...")
      for (rtd <- retransmissionList)
        channel.send(rtd.data, rtd.receiverAddress, rtd.receiverPort)
  }
}

// may be master
class ClientListener(name: String, channel: UDPChannel, router: ActorRef) extends Actor {
  def active: Receive = {
    case "listen" =>
      println(s"[$name] : Listening ${channel.address}:${channel.port}...")
      while (true) {
        val mayBeRUDPData = channel.receive()
        mayBeRUDPData match {
          case Some(data) =>
            router ! CalculateAndSend(data)
          case _ =>
        }
      }

    case "sleep" =>
      context become inactive
  }

  def inactive: Receive = {
    case "wake up" =>
      context become active
  }

  def receive = active
}

object Client1Demo {
  @throws[Exception]
  def main(args: Array[String]) {
    val channel = new UDPChannel(port = 9875, mtu = 4096, timeout = 3000)

    val system = ActorSystem("Client-1")
    implicit val executor = system.dispatcher

    val router = system.actorOf(RoundRobinPool(8).props(Props(classOf[Calculator], channel)), name = "Router")
    val listener = system.actorOf(Props(classOf[ClientListener], "Client Listener", channel, router), name = "ClientListenerActor")
    val statusChecker = system.actorOf(Props(classOf[StatusChecker], channel, router), name = "ClientStatusCheckerActor")

    listener ! "listen"
    system.scheduler.schedule(2.seconds, 10.seconds, statusChecker, "check")
  }
}

