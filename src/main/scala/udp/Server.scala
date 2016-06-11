package udp

import java.net._
import java.util.Date
import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import akka.routing.RoundRobinPool
import messages._
import scala.collection.mutable.ListBuffer
import matrix.Matrix
import tasks._
import common.Serializer

/**
  * Created by taner.gokalp on 07/06/16.
  */

class ServerWorker(socket: DatagramSocket, master: ActorRef) extends Actor with Serializer {
  private val timeout = 1000

  def active: Receive = {
    case SendToClient(task, address, port) =>
      println(s"[${self.path.toString}] sending server packet to client with (Seq: ${task.order})")
      val serverPacket = new ServerPacket(task.order, System.currentTimeMillis(), task)
      val packet = serializer(serverPacket)
      socket.send(new DatagramPacket(packet, packet.length, address, port))

    case ReceiveFromClient(packet) =>
      val clientPacket = deserializer(packet.getData).asInstanceOf[ClientPacket]

      println(s"[${self.path.toString}] received client packet from client with (Seq: ${clientPacket.completedTask.order}, Result: ${clientPacket.completedTask.result})")
      master ! Result(clientPacket.completedTask)
  }

  def receive = active
}

object ServerWorker {
  def apply(socket: DatagramSocket, master: ActorRef): Props = Props(classOf[ServerWorker], socket, master)
}

/**************************************************************************************************************************************************************/
class ServerMaster(masterSystem: ActorSystem, server: Host, remoteData: (InetAddress, Int)) extends Actor {
  val matrixSize = 4
  val matrix = Matrix.random(matrixSize, withLogging = true)
  val transposedMatrix = Matrix.transpose(matrix, withLogging = true)
  val distribution = Matrix.distribute(matrix, transposedMatrix, withLogging = false)

  var results = ListBuffer.empty[CompletedTask]

  val socket = new DatagramSocket(server.port)
  val poolSize = 4 // val numberOfProcessors = Runtime.getRuntime.availableProcessors
  val system = ActorSystem("ServerMaster")
  val router = system.actorOf(RoundRobinPool(poolSize).props(ServerWorker(socket, self)), name = "ServerSlave")

  // todo implement active inactive contexts
  def receive = {
    case Start(d) =>
      try {
        for (i <- distribution.indices) {
          router ! SendToClient(distribution(i), remoteData._1, remoteData._2)

          val receivePacket = new DatagramPacket(new Array[Byte](server.maxDataSize), server.maxDataSize)
          socket.receive(receivePacket)

          router ! ReceiveFromClient(receivePacket)
        }

      } catch {
        case e: Exception => e.printStackTrace()
      }

    case Result(ct) =>
      results += ct

      if (results.size == matrixSize * matrixSize) {
        println(s"Computation Completed...")
        router ! Terminate
        Thread.sleep(1000)
        val resultMatrix = results.sortBy(_.order).map(_.result).toVector.grouped(matrixSize).toVector
        Matrix.print(resultMatrix)
        socket.close()
        system.terminate()
        masterSystem.terminate()
      }
  }
}

/**************************************************************************************************************************************************************/
object Server extends Serializer {
  @throws[Exception]
  def main(args: Array[String]) {
    val server = new Host(port = 9876)
    val remoteData = (InetAddress.getLocalHost, 9875) // todo : remove

    val system = ActorSystem("Server")
    val masterActor = system.actorOf(Props(classOf[ServerMaster], system, server, remoteData), name = "ServerMaster")
    println("server started at port 9876...")

    masterActor ! Start(new Date())
  }
}
