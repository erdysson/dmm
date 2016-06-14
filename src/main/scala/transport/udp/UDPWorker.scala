package transport.udp

import akka.actor.{Actor, ActorRef, Props}
import messages.{ReTransmit, Result}

/**
  * Created by taner.gokalp on 14/06/16.
  */

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