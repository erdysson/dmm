package demo

import application.tasks.Task
import transport.rudp.{RUDPData, UDPChannel}

/**
  * Created by taner.gokalp on 14/06/16.
  */
object Client1Demo {
  @throws[Exception]
  def main(args: Array[String]) {
    // val config = (InetAddress.getByName("localhost"), 9875)
    // val udpChannel = UDPChannel(config._1, config._2, 1024)
    // val masterSystem = ActorSystem("Client1")
    // val masterActor = masterSystem.actorOf(Client("WorkerPool", udpChannel, 4))
    // masterActor ! Listen()

    val channel = new UDPChannel(port = 9875, mtu = 4096, timeout = 3000)
    println(s"Client started at 9875...")
    while (true) {
      val received = channel.receive()
      received.isEmpty match {
        case false =>
          val data = received.get
          println(s"Client received data : $data")
          val rudpData = new RUDPData(data.data.asInstanceOf[Task].complete(), data.receiverAddress, data.receiverPort)
          channel.send(rudpData)
        case _ =>
      }
    }
  }
}

