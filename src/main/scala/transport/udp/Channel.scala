package transport.udp

import java.net.{DatagramPacket, InetAddress, DatagramSocket}

import transport.utils.Serializer

/**
  * Created by taner.gokalp on 13/06/16.
  */

private case class WaitingUDPPacket(timestamp: Long, data: AnyRef)
case class UDPPacket(seq: Int, ack: Int, data: AnyRef)

class UDPChannel(val address: InetAddress, val port: Int, mtu: Int) extends Serializer {
  private val socket = new DatagramSocket(port)

  private val timeout = 4000
  private val packetMap = scala.collection.mutable.Map.empty[Int, WaitingUDPPacket]

  private var seqCounter = 0
  private var ackCounter = 0

  def send(data: AnyRef): Unit = {
    val dataAsUDPPacket = new UDPPacket(seq = seqCounter, ack = ackCounter, data)
    val dataAsByteStream = serialize(dataAsUDPPacket)
    val dataAsDatagramPacket = new DatagramPacket(dataAsByteStream, dataAsByteStream.length)

    packetMap += (seqCounter -> new WaitingUDPPacket(System.currentTimeMillis(), data))
    println(s"Adding packet to map with sequence number $seqCounter")

    println(s"$address:$port sending $data as datagram packet with sequence number $seqCounter...")
    socket.send(dataAsDatagramPacket)
    seqCounter += 1
  }

  def listen(): DatagramPacket = {
    val packet = new DatagramPacket(new Array[Byte](mtu), mtu)
    socket.receive(packet)
    packet
  }

  def receive(dataAsDatagramPacket: DatagramPacket) = {
    val dataAsByteStream = dataAsDatagramPacket.getData
    val udpPacket = deserialize(dataAsByteStream).asInstanceOf[UDPPacket]

    val ack = udpPacket.ack
    packetMap.get(ack) match {
      case Some(packet) =>
        println(s"removing packet from map with sequence number $ack")
        packetMap -= ack
        println(s"Packet map status : $packetMap")
        ackCounter = ack
      case None =>
        println(s"dropping packet which does not exist with sequence number $ack")
        // todo : add return value inside pattern-matching
    }
    udpPacket
  }

  def checkStackStatus(): List[AnyRef] = {
    println("checking map status...")
    packetMap.isEmpty match {
      case true =>
        println(s"There is no packet waiting to be acked in the map...")
        List.empty[AnyRef]
      case false =>
        val packetsNeedsToBeReTransmitted = packetMap.values.filter(packet => System.currentTimeMillis() - packet.timestamp > timeout).toList
        if (packetsNeedsToBeReTransmitted.isEmpty) {
          println(s"There is no packet timed out in the map...")
          List.empty[AnyRef]
        } else {
          val retransmittedData = packetsNeedsToBeReTransmitted.map(p => p.data)
          println(s"Packets need to be re-transmitted : $retransmittedData")
          retransmittedData
        }
    }
  }
}