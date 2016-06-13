package comm

import java.net._

/**
  * Created by taner.gokalp on 11/06/16.
  */

class Server(override val name: String, override val address: InetAddress, override val port: Int) extends Host(name, address, port) {
  private val socket = new DatagramSocket(port)
  private var SEQCounter = 0

  private var stack: List[ServerPacket] = List.empty[ServerPacket]
  private var resultStack: List[DummyResult] = List.empty[DummyResult]

  def sendServerPacket(ACK: Int = 0, FIN: Int, dummyData: DummyData, remoteData: Tuple2[InetAddress, Int]): Unit = {
    val packet = new ServerPacket(SEQ = SEQCounter, ACK = ACK, FIN = FIN, dummyData)

    stack = packet :: stack
    println(s"Stack Status On Sending : $stack")

    val packetAsByteStream = serializer(packet)
    val packetAsDatagram = new DatagramPacket(packetAsByteStream, packetAsByteStream.length, remoteData._1, remoteData._2)

    SEQCounter += 1

    socket.send(packetAsDatagram)

    /* listen now */
    val receivePacket = new DatagramPacket(new Array[Byte](maxDataSize), maxDataSize)
    socket.receive(receivePacket)

    val receivePacketAsByteStream = receivePacket.getData
    val receivedPacket = deserializer(receivePacketAsByteStream).asInstanceOf[ClientPacket]

    val lastItemInStack = stack.head

    if (lastItemInStack.SEQ == packet.ACK) {
      println(s"Popping Item from Stack : $lastItemInStack")
      stack = stack.tail
      println(s"Stack Status On Receiving : $stack")
      resultStack = receivedPacket.dummyResult :: resultStack
      println(s"Result Stack Status After Receive : $resultStack")
    } else {
      println(s"Sequence numbers does not match!!!!")
    }

    if (lastItemInStack.FIN == 1) {
      println(s"Terminating Communication")
      socket.close()
    }

  }

  def extractAndSendPacket(datagramPacket: DatagramPacket, FIN: Int, dummyData: DummyData, remoteData: Tuple2[InetAddress, Int]): Unit = {
    val packetAsByteStream = datagramPacket.getData
    val packet = deserializer(packetAsByteStream).asInstanceOf[ClientPacket]

    val lastItemInStack = stack.head

    if (lastItemInStack.SEQ == packet.ACK) {
      println(s"Popping Item from Stack : $lastItemInStack")
      stack = stack.tail
      println(s"Stack Status On Receiving : $stack")
      resultStack = packet.dummyResult :: resultStack
      println(s"Result Stack Status After Receive : $resultStack")
    } else {
      println(s"Sequence numbers does not match!!!!")
    }

    if (lastItemInStack.FIN == 1) {
      println(s"Terminating Communication")
      socket.close()
    } else {
      // sendServerPacket(FIN = FIN, ACK = packet.SEQ, dummyData = dummyData, remoteData = remoteData)

      val newPacket = new ServerPacket(SEQ = SEQCounter, ACK = packet.SEQ, FIN = FIN, dummyData)

      stack = newPacket :: stack
      println(s"Stack Status On Sending : $stack")

      val packetAsByteStream = serializer(newPacket)
      val packetAsDatagram = new DatagramPacket(packetAsByteStream, packetAsByteStream.length, remoteData._1, remoteData._2)

      SEQCounter += 1

      socket.send(packetAsDatagram)
    }
  }

  def process(dummyDataList: List[DummyData], remoteData: Tuple2[InetAddress, Int]): Unit = {
    val timeout = 2000

    println(s"TASK LIST => $dummyDataList")

    if (dummyDataList.isEmpty) {
      println(s"Server finished jobs")
      println(s"Results : $resultStack")
    } else {
      var running = true
      val serverPacketAsByteStream = serializer(dummyDataList.head)
      val serverPacketAsDatagram = new DatagramPacket(serverPacketAsByteStream, serverPacketAsByteStream.length, remoteData._1, remoteData._2)

      println(s"Sending packet => ${dummyDataList.head}")
      socket.send(serverPacketAsDatagram)
      val receivePacket = new DatagramPacket(new Array[Byte](maxDataSize), maxDataSize)

      socket.setSoTimeout(timeout)
      while (running) {
        try {
          println("waiting...")
          socket.receive(receivePacket)
          val receivedPacketAsByteStream = receivePacket.getData
          val receivedPacket = deserializer(receivedPacketAsByteStream).asInstanceOf[DummyResult]
          println(s"received packet => $receivedPacket")
          resultStack = receivedPacket :: resultStack
          running = false
          process(dummyDataList.tail, remoteData)

        } catch {
          case ste: SocketTimeoutException =>
            println("PACKET HAS TIMED OUT.... PROCESSING THE PACKET AGAIN...!")
            println(s"Re-Sending packet => ${dummyDataList.head}")
            socket.send(serverPacketAsDatagram)
            // process(dummyDataList, remoteData)
        }
      }
    }
  }

  def start(list: List[DummyData]): Unit = {

  }

}

object Server {
  @throws[Exception]
  def main(args: Array[String]) {
    val clientData = (InetAddress.getLocalHost, 9876)
    val server = new Server("server", InetAddress.getLocalHost, 9874)

    val dataList = List.tabulate(5, 5)((i, j) => (Math.random() * (i+j) + 1).toInt)
    var dummyDataList = List.empty[DummyData]

    println(s"Server started on port ${server.port}...")
    for (i <- dataList.indices) {
      dummyDataList = new DummyData(i, dataList(i)) :: dummyDataList
    }

    server.process(dummyDataList, clientData)

//    try {
////        val sendData = new DummyData(i, dataList(i))
////        val FIN = if (i == dataList.length - 1) 1 else 0
////
////        Thread.sleep(1000)
////
////        server.sendServerPacket(ACK = 0, FIN = FIN,sendData, clientData)
////
////        val receivePacket = new DatagramPacket(new Array[Byte](server.maxDataSize), server.maxDataSize)
////        server.socket.receive(receivePacket)
////        server.extractAndSendPacket(receivePacket, FIN = FIN, sendData, clientData)
//
//    } catch {
//      case se: SocketException =>
//        se.printStackTrace()
//        server.socket.close()
//    }

    Thread.sleep(1000)
    println(s"Server is being closed..")

  }
}