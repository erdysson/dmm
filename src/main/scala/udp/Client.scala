package udp

import java.net.{SocketException, DatagramPacket, DatagramSocket}
import actors.Worker
import akka.actor._
import akka.routing.BalancingPool
import messages.ReceivePacket

/**
  * Created by taner.gokalp on 07/06/16.
  */

// todo : separate receiver and sender groups maybe ?
// todo : create mixin class composition with worker for ack sender / receiver
// todo : write start script for all instances

object Client {
  val system = ActorSystem("Client")
  val client = new Host(port = 9875)
  val socket = new DatagramSocket(client.port)
  val numberOfProcessors = Runtime.getRuntime.availableProcessors
  val router = system.actorOf(BalancingPool(numberOfProcessors).props(Worker(socket)), "clientRouter")

  @throws[Exception]
  def main(args: Array[String]) {
    println("Client listening on port 9875...")
    try {
      while (true) {
        val receivePacket = new DatagramPacket(new Array[Byte](client.maxDataSize), client.maxDataSize)
        socket.receive(receivePacket)
        router ! ReceivePacket(receivePacket)
      }
    } catch {
      case e: SocketException =>
        println("client socket is being closed...")
        socket.close()
        system.terminate()
    }
  }
}