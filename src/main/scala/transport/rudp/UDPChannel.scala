package transport.rudp

import java.net.{DatagramPacket, DatagramSocket, InetAddress}
import java.util.concurrent.atomic.AtomicInteger
import transport.utils.Serializer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by taner.gokalp on 15/06/16.
  */

abstract class Packet
case class AckPacket(ack: Int) extends Packet
case class DataPacket(seq: Int, data: Any) extends Packet

case class RUDPData(data: Any, receiverAddress: InetAddress, receiverPort: Int)

class UDPChannel(val address: InetAddress = InetAddress.getByName("localhost"), val port: Int, mtu: Int, timeout: Int) extends Serializer {
  private val socket = new DatagramSocket(port)
  private val packetMap = mutable.Map.empty[Int, (Long, RUDPData)] // Sequence Number -> (Timestamp, Data)

  private val seq: AtomicInteger = new AtomicInteger(0)

  def send(rudpData: RUDPData): Unit = { // todo : add chunk ability
    val dataAsPacket = new DataPacket(seq.getAndIncrement(), rudpData.data)
    // add data packet to the un-acked packet map
    packetMap(dataAsPacket.seq) = (System.currentTimeMillis(), rudpData)
    sendPacket(dataAsPacket, rudpData.receiverAddress, rudpData.receiverPort, needstoBeAcked = true)
  }

  def receive(): Option[RUDPData] = {
    val dataAsDatagramPacket = new DatagramPacket(new Array[Byte](mtu), mtu)
    socket.receive(dataAsDatagramPacket)

    val dataAsByteStream = dataAsDatagramPacket.getData
    val packet = deserialize(dataAsByteStream).asInstanceOf[Packet]
    // match if the received packet is an ack packet or data packet
    packet match {
      case a: AckPacket =>
        removeFromPacketMap(a.ack)
        println(s"ack received for seq ${a.ack}")
        None
      case d: DataPacket =>
        println(s"channel at $address: $port received ${dataAsByteStream.length} byte data from ${dataAsDatagramPacket.getAddress}:${dataAsDatagramPacket.getPort}")
        sendPacket(new AckPacket(d.seq), dataAsDatagramPacket.getAddress, dataAsDatagramPacket.getPort, needstoBeAcked = false)
        Some(new RUDPData(d.data, dataAsDatagramPacket.getAddress, dataAsDatagramPacket.getPort))
      case _ =>
        println("Unexpected packet type. Probably corrupted...")
        None
    }
  }

  def checkStatus(): Option[List[RUDPData]] = {
    packetMap.isEmpty match {
      case true => None
      case _ =>
        val packetsToBeRetransmitted = mutable.ListBuffer.empty[RUDPData]
        packetMap.dropWhile(p => {
          val isTimedOut = System.currentTimeMillis() - p._2._1 >= timeout
          if (isTimedOut) {
            val rudpData = p._2._2
            packetsToBeRetransmitted.append(rudpData)
          }
          isTimedOut
        })
        if (packetsToBeRetransmitted.isEmpty) None else Some(packetsToBeRetransmitted.toList)
    }
  }

  private def sendPacket(packet: Packet, receiverAddress: InetAddress, receiverPort: Int, needstoBeAcked: Boolean = true): Unit = {
    val dataAsByteStream = serialize(packet)
    val dataAsDatagramPacket = new DatagramPacket(dataAsByteStream, dataAsByteStream.length, receiverAddress, receiverPort)
    socket.send(dataAsDatagramPacket)
    println(s"channel at $address:$port sending ${dataAsByteStream.length} byte data to $receiverAddress:$receiverPort")
  }

  private def removeFromPacketMap(ack: Int): Unit = {
    packetMap.get(ack) match {
      case Some(a) =>
        packetMap.remove(ack)
        println(s"packet with seq $ack is acked!")
      case None =>
        println(s"packet with seq $ack is not in the un-acked map!")
    }
  }
}
