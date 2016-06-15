package transport.udp.channel

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import transport.udp.{UDPPacket, WaitingUDPPacket}
import transport.utils.Serializer

/**
  * Created by taner.gokalp on 13/06/16.
  */

class UDPChannel(val address: InetAddress, val port: Int, mtu: Int) extends Serializer {
  private val socket = new DatagramSocket(port)
  // todo : parametrize timeout value
  private val timeout = 5000
  private var packetMap = scala.collection.mutable.Map.empty[Int, WaitingUDPPacket]

  private val seq: AtomicInteger = new AtomicInteger(0)
  @volatile private var seqCounter = 0
  private var ackCounter = 0

  def send(data: Any, remoteConfig: (InetAddress, Int)): Unit = {
    val dataAsUDPPacket = new UDPPacket(seq = seqCounter, ack = ackCounter, data)
    val dataAsByteStream = serialize(dataAsUDPPacket)
    val dataAsDatagramPacket = new DatagramPacket(dataAsByteStream, dataAsByteStream.length, remoteConfig._1, remoteConfig._2)

    packetMap += (seqCounter -> new WaitingUDPPacket(System.currentTimeMillis(), data, remoteConfig))
    seqCounter += 1
    println(s"Adding packet to map with sequence number ${seq.addAndGet(1)}, $seq")

    println(s"Sending to $remoteConfig")

    socket.send(dataAsDatagramPacket)
  }

  def listen(): DatagramPacket = {
    val packet = new DatagramPacket(new Array[Byte](mtu), mtu)
    socket.receive(packet)
    println(s"Channel received packet $packet")
    packet
  }

  def receive(dataAsDatagramPacket: DatagramPacket): Option[UDPPacket] = {
    val dataAsByteStream = dataAsDatagramPacket.getData
    val udpPacket = deserialize(dataAsByteStream).asInstanceOf[UDPPacket]

    println(s"received packet : $udpPacket")

    val ack = udpPacket.ack
    packetMap.get(ack) match {
      case Some(packet) =>
        println(s"removing packet from map with sequence number $ack")
        packetMap -= ack
        println(s"Packet map status : $packetMap")
        ackCounter = ack
        Some(udpPacket)
      case None =>
        println(s"dropping packet which does not exist with sequence number $ack")
        None
        // todo : drop only on server
    }
  }

  def checkStackStatus(): List[Any] = {
    println("checking map status...")
    packetMap.isEmpty match {
      case true =>
        println(s"There is no packet waiting to be acked in the map...")
        List.empty[Any]
      case false =>
        val packetsNeedsToBeReTransmitted = scala.collection.mutable.ListBuffer.empty[Any]
        packetMap = packetMap.filter(waitingUdpPacket => {
          System.currentTimeMillis() - waitingUdpPacket._2.timestamp >= timeout match {
            case true => packetsNeedsToBeReTransmitted += (waitingUdpPacket._2.data, waitingUdpPacket._2.remoteConfig); false
            case _ => true
          }
        })

        packetsNeedsToBeReTransmitted.isEmpty match {
          case true => println(s"There is no packet timed out in the map..."); List.empty[Any]
          case false => println(s"Packets need to be re-transmitted : $packetsNeedsToBeReTransmitted"); packetsNeedsToBeReTransmitted.toList
        }
    }
  }
}

object UDPChannel {
  def apply(address: InetAddress, port: Int, mtu: Int) = new UDPChannel(address, port, mtu)
}
