package demo

import akka.actor.{Props, ActorRef, ActorSystem, Actor}
import akka.routing.RoundRobinPool
import application.tasks.Task
import transport.rudp.{RUDPData, UDPChannel}

/**
  * Created by taner.gokalp on 14/06/16.
  */

case class CalculateAndSend(rudpData: RUDPData)

class StatusChecker(channel: UDPChannel, updateInterval: Int, master: ActorRef) extends Actor {
  override def preStart(): Unit = {
    self ! "check"
  }

  def checkStatus(): Unit = {
    val packetsToBeReTransmitted = channel.checkStatus()
    packetsToBeReTransmitted match {
      case Some(retransmissionList) =>
        master ! "re-transmit" // todo : add case class
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
      self ! "check"
  }

  def receive = active
}

class Calculator(channel: UDPChannel) extends Actor {
  def receive = {
    case CalculateAndSend(rudpData) =>
      println("calculator actor calculating...")
      channel.send(rudpData.data.asInstanceOf[Task].complete(), rudpData.receiverAddress, rudpData.receiverPort)
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
    val router = system.actorOf(RoundRobinPool(4).props(Props(classOf[Calculator], channel)), name = "Router")
    val listener = system.actorOf(Props(classOf[ClientListener], "Client Listener", channel, router), name = "ClientListenerActor")
    listener ! "listen"

  }
}

