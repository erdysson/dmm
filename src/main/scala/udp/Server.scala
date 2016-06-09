package udp

import java.net._
import akka.actor.{Props, ActorSystem}
import akka.routing.BalancingPool
import scala.collection.mutable.ListBuffer
import matrix.Matrix
import tasks.CompletedTask

/**
  * Created by valhalla on 07/06/16.
  */

/*
  - todo : client distributor router actor system -> in main [actor group having the information of the host]
  - todo : result calculator router actor system -> common, shared [actor pool] to collect data and handle
  - todo : ack listener -> [actor pool] to listen acks and verify / detect packets
 */

object Server extends Serializer {
  val system = ActorSystem("Server")
  val server = new Host(port = 9876)
  val socket = new DatagramSocket(server.port)
  val numberOfProcessors = Runtime.getRuntime.availableProcessors
  val router = system.actorOf(BalancingPool(numberOfProcessors).props(Props(classOf[Worker], socket)), "serverRouter")

  @throws[Exception]
  def main(args: Array[String]) {
    // todo : remove
    val (address, port) = (InetAddress.getLocalHost, 9875)
    val matrix = Matrix.random(4, withLogging = false)
    val transposedMatrix = Matrix.transpose(matrix, withLogging = false)
    val distribution = Matrix.distribute(matrix, transposedMatrix, withLogging = false)
    // todo : figure out how to collect results
    // todo : figure out client existance check

    // todo : move into actor logic
    @volatile var results = ListBuffer.empty[CompletedTask]

    println("server started at port 9875...")
    println("distribution will be started in 1 second...")
    Thread.sleep(1000)

    try {
      for (i <- distribution.indices) {
        router ! SendPacket(distribution(i), address, port)

        val receivePacket = new DatagramPacket(new Array[Byte](server.maxDataSize), server.maxDataSize)
        socket.receive(receivePacket)

        router ! ReceivePacket(receivePacket)
      }
    } catch {
      case e: SocketException => e.printStackTrace()
    }
  }
}