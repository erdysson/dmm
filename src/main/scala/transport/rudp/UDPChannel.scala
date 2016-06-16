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
case class FinPacket(fin: Int) extends Packet // todo : use it
case class AckPacket(ack: Int) extends Packet
case class DataPacket(seq: Int, data: Any) extends Packet
case class RUDPData(data: Any, receiverAddress: InetAddress, receiverPort: Int)
case class Fragment(seq: Int, fSeq: Int, fCount: Int, bytes: Array[Byte]) extends Packet

class UDPChannel(val address: InetAddress = InetAddress.getByName("localhost"), val port: Int, mtu: Int, timeout: Int) extends Serializer {
  private val socket = new DatagramSocket(port)
  private val packetMap = mutable.Map.empty[Int, (Long, RUDPData)] // Sequence Number -> (Timestamp, Data)
  private var fragmentMap = mutable.Map.empty[Int, ListBuffer[Fragment]]

  private val seq: AtomicInteger = new AtomicInteger(0)

  def send(data: Any, receiverAddress: InetAddress, receiverPort: Int): Unit = {
    val dataAsPacket = new DataPacket(seq.getAndIncrement(), data)
    packetMap(dataAsPacket.seq) = (System.currentTimeMillis(), new RUDPData(data, receiverAddress, receiverPort)) // server does not need receiver data !
    sendPacket(dataAsPacket, receiverAddress, receiverPort)
  }

  def receive(): Option[RUDPData] = {
    val dataAsDatagramPacket = new DatagramPacket(new Array[Byte](mtu), mtu)
    socket.receive(dataAsDatagramPacket)

    val dataAsByteStream = dataAsDatagramPacket.getData
    val packet = deserialize(dataAsByteStream).asInstanceOf[Packet]

    packet match {
      case a: AckPacket =>
        removeFromPacketMap(a.ack)
        None

      case d: DataPacket =>
        sendPacket(new AckPacket(d.seq), dataAsDatagramPacket.getAddress, dataAsDatagramPacket.getPort)
        Some(new RUDPData(d.data, dataAsDatagramPacket.getAddress, dataAsDatagramPacket.getPort))

      case f: Fragment =>
        val fragment = packet.asInstanceOf[Fragment]

        fragmentMap.get(fragment.seq) match {
          case Some(listBuffer) =>
            listBuffer += fragment

            val allFragmentsReceived = listBuffer.length == fragment.fCount
            allFragmentsReceived match {
              case true =>
                sendPacket(new AckPacket(fragment.seq), dataAsDatagramPacket.getAddress, dataAsDatagramPacket.getPort)

                var completeByteArray = Array.empty[Byte]
                listBuffer.sortBy(_.fSeq).foreach(frag => completeByteArray ++= frag.bytes)
                val dataPacket = deserialize(completeByteArray).asInstanceOf[DataPacket]

                fragmentMap -= fragment.seq
                Some(new RUDPData(dataPacket.data, dataAsDatagramPacket.getAddress, dataAsDatagramPacket.getPort))

              case false => None
            }

          case None =>
            fragmentMap(fragment.seq) = ListBuffer(fragment)
            None
        }

      case _ =>
        println("Unexpected packet type. Probably corrupted in fragmentation...")
        None
    }
  }

  def checkStatus(): Option[List[RUDPData]] = {
    packetMap.isEmpty match {
      case false =>
        val packetsToBeRetransmitted = mutable.ListBuffer.empty[RUDPData]
        packetMap.retain((seq, rest) => {
          val isTimedOut = System.currentTimeMillis() - rest._1 >= timeout
          if (isTimedOut)
            packetsToBeRetransmitted.append(rest._2)
          !isTimedOut
        })
        if (packetsToBeRetransmitted.isEmpty) None else Some(packetsToBeRetransmitted.toList)

      case true => None
    }
  }

  private def fragment(seq: Int, bytes: Array[Byte]): Array[Fragment] = {
    val fragments = bytes.grouped(mtu - 114).toArray // 114 bytes for fragment class byte array length
    Array.tabulate(fragments.length)(i => new Fragment(seq, i, fragments.length, fragments(i)))
  }

  private def sendPacket(packet: Packet, receiverAddress: InetAddress, receiverPort: Int): Unit = {
    val dataAsByteStream = serialize(packet)

    val isPacketLargerThanMTU = dataAsByteStream.length > mtu
    isPacketLargerThanMTU match {
      case true =>
        val fragments = fragment(packet.asInstanceOf[DataPacket].seq, dataAsByteStream)
        fragments.foreach(frag => {
          val fragmentByteStream = serialize(frag)
          socket.send(new DatagramPacket(fragmentByteStream, fragmentByteStream.length, receiverAddress, receiverPort))
        })
      case false =>
        val dataAsDatagramPacket = new DatagramPacket(dataAsByteStream, dataAsByteStream.length, receiverAddress, receiverPort)
        socket.send(dataAsDatagramPacket)
    }
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
