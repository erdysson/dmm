package transport.udp.worker

import java.net.InetAddress

import akka.actor.{Actor, ActorRef, Props}
import messages.{CheckStatus, WakeUp, Sleep}
import transport.udp.UDPPacket
import transport.udp.channel.UDPChannel

/**
  * Created by taner.gokalp on 14/06/16.
  */

case class SendToClient(data: Any)
case class ReTransmitToClient(dataList: List[Any])
case class ResultFromClient(udpPacket: UDPPacket)
case class ReceiveFromClient()

class ServerWorker(val channel: UDPChannel, remoteConfig: (InetAddress, Int), master: ActorRef) extends Actor {

  def active: Receive = {

    case Sleep(now) =>
      println(s"Actor system is deactivated... $now")
      context become inactive

    case SendToClient(data) =>
      channel.send(data, remoteConfig)

    case ReceiveFromClient() =>
      val udpPacket = channel.receive(channel.listen())
      udpPacket match {
        case Some(packet) => master ! ResultFromClient(packet)
        case None => println(s"Packet is not relevant")
      }

    case CheckStatus() =>
      val dataToBeReTransmitted = channel.checkStackStatus()
      if (dataToBeReTransmitted.nonEmpty) {
        val listOfData = dataToBeReTransmitted.asInstanceOf[List[(Any, (InetAddress, Int))]].map(_._1)
        master ! ReTransmitToClient(listOfData)
      }
  }

  def inactive: Receive = {
    case WakeUp(now) =>
      println(s"Actor system is activated... $now")
      context become active
  }

  def receive = active
}

object ServerWorker {
  def apply(channel: UDPChannel, remoteConfig: (InetAddress, Int), master: ActorRef): Props = Props(classOf[ServerWorker], channel, remoteConfig, master)
}