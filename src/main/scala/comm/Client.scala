package comm

import java.net.{DatagramSocket, SocketException, DatagramPacket, InetAddress}

/**
  * Created by taner.gokalp on 11/06/16.
  */

class Client(override val name: String, override val address: InetAddress, override val port: Int) extends Host(name, address, port) {
  private val socket = new DatagramSocket(port)
  private var SEQCounter = 0
  var listening = true

  def calculate(data: DummyData): DummyResult = new DummyResult(data.order, data.data.sum)

  def extractAndSendPacket(datagramPacket: DatagramPacket): Unit = {
    val packetAsByteStream = datagramPacket.getData
    val serverPacket = deserializer(packetAsByteStream).asInstanceOf[ServerPacket]

    println(s"Client received packet : $serverPacket")

    val FIN = if (serverPacket.FIN == 1) 1 else 0
    val clientPacket = new ClientPacket(SEQ = SEQCounter, ACK = serverPacket.SEQ , FIN = FIN, calculate(serverPacket.dummyData))
    SEQCounter += 1
    val clientPacketAsByteStream = serializer(clientPacket)
    val clientPacketAsDatagram = new DatagramPacket(clientPacketAsByteStream, clientPacketAsByteStream.length, datagramPacket.getAddress, datagramPacket.getPort)

    println(s"Client sending packet : $clientPacket")

    socket.send(clientPacketAsDatagram)

    if (FIN == 1) {
      println(s"Client will be terminated due to FIN bit = 1...")
      socket.close()
      listening = false
    }
  }
}

object Client {
  @throws[Exception]
  def main(args: Array[String]) {
    val client = new Client("client", InetAddress.getLocalHost, 9876)

    println(s"Client listening on port ${client.port}...")
    val receivePacket = new DatagramPacket(new Array[Byte](client.maxDataSize), client.maxDataSize)

    while (client.listening) {
      try {

        client.socket.receive(receivePacket)
        val receivedPacketAsByteStream = receivePacket.getData
        val dummyData = client.deserializer(receivedPacketAsByteStream).asInstanceOf[DummyData]
        println(s"Client received packet : $dummyData")

        val randomTimeout = (Math.random() * 3 + 1).toInt
        println(s"random timeout => $randomTimeout")

        if (randomTimeout < 3) {
          Thread.sleep(randomTimeout * 1000)
          val sentData = client.calculate(dummyData)
          val sentDataAsByteStream = client.serializer(sentData)
          val sentDataAsDatagram = new DatagramPacket(sentDataAsByteStream, sentDataAsByteStream.length, receivePacket.getAddress, receivePacket.getPort)
          client.socket.send(sentDataAsDatagram)

        } else
          println("Client dropped packet due to timeout...")

        // Thread.sleep(250)

      } catch {
        case se: SocketException =>
          se.printStackTrace()
          client.listening = false
          client.socket.close()
      }
    }

    Thread.sleep(1000)
    println(s"Client is being closed...")
  }
}