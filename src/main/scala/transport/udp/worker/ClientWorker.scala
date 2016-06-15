package transport.udp.worker

import java.net.InetAddress

import akka.actor.{Props, Actor, ActorRef}
import messages.{CheckStatus, Sleep, WakeUp}
import transport.udp.UDPPacket
import transport.udp.channel.UDPChannel

/**
  * Created by taner.gokalp on 15/06/16.
  */

case class SendToServer(data: Any, remoteConfig: (InetAddress, Int))
case class ReTransmitToServer(configuredReTransmissionList: List[(Any, (InetAddress, Int))])
case class ResultFromServer(udpPacket: UDPPacket, remoteConfig: (InetAddress, Int))
case class ReceiveFromServer()

class ClientWorker(val channel: UDPChannel, master: ActorRef) extends Actor {

  def active : Receive = {

    case Sleep(now) =>
      println(s"Actor system is deactivated... $now")
      context become inactive

    case SendToServer(data, remoteConfig) =>
      channel.send(data, remoteConfig)

    case ReceiveFromServer() =>
      val datagramPacket = channel.listen()
      val udpPacket = channel.receive(datagramPacket)
      println(s"Client received from server $udpPacket")
      udpPacket match {
        case Some(packet) => master ! ResultFromServer(packet, (datagramPacket.getAddress, datagramPacket.getPort))
        case None => println(s"Packet is not relevant")
      }

    case CheckStatus() =>
      val dataToBeReTransmitted = channel.checkStackStatus()
      if (dataToBeReTransmitted.nonEmpty) {
        val listOfData = dataToBeReTransmitted.asInstanceOf[List[(Any, (InetAddress, Int))]]
        master ! ReTransmitToServer(listOfData)
      }
  }

  def inactive: Receive = {
    case WakeUp(now) =>
      println(s"Actor system is activated... $now")
      context become active
  }

  def receive = active
}

object ClientWorker {
  def apply(channel: UDPChannel, master: ActorRef): Props = Props(classOf[ClientWorker], channel, master)
}