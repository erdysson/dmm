package udp

import java.net._
import java.util.Date
import akka.actor.{ActorRef, Props, Actor, ActorSystem}
import akka.routing.RoundRobinPool
import messages._
import scala.collection.SortedMap
import scala.collection.mutable.ListBuffer
import matrix.Matrix
import tasks.CompletedTask
import common.Serializer

/**
  * Created by taner.gokalp on 07/06/16.
  */

class Packet()

class ServerWorker(socket: DatagramSocket, master: ActorRef) extends Actor with Serializer {

  def receive = {
    case SendToClient(task, address, port) =>
      println(s"[${self.path.toString}] sending task to client with (Seq: ${task.order})")
      val packet = serializer(task)
      socket.send(new DatagramPacket(packet, packet.length, address, port))

    case ReceiveFromClient(packet) =>
      val completedTask = deserializer(packet.getData).asInstanceOf[CompletedTask]
      println(s"[${self.path.toString}] received completed task from client with (Seq: ${completedTask.order}, Result: ${completedTask.result})")
      master ! Result(completedTask)
  }
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
