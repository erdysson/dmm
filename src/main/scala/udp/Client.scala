package udp

import java.net.{SocketException, DatagramPacket, DatagramSocket}
import java.util.Date
import akka.actor._
import akka.routing.RoundRobinPool
import common.Serializer
import messages._
import tasks.Task

/**
  * Created by taner.gokalp on 07/06/16.
  */

class ClientWorker(socket: DatagramSocket, master: ActorRef) extends Actor with Serializer {

  def receive = {
    case ReceiveFromServer(packet) =>
      val task: Task = deserializer(packet.getData).asInstanceOf[Task]
      println(s"[${self.path.toString}] received task from server with (Seq: ${task.order})")
      val completedTask = Task.complete(task)

      self ! SendToServer(completedTask, packet.getAddress, packet.getPort)

    case SendToServer(completedTask, address, port) =>
      println(s"[${self.path.toString}] sending completed task to server with (Seq: ${completedTask.order}, Result: ${completedTask.result})")
      val replyPacket = serializer(completedTask)
      socket.send(new DatagramPacket(replyPacket, replyPacket.length, address, port))
  }
}

object ClientWorker {
  def apply(socket: DatagramSocket, master: ActorRef): Props = Props(classOf[ClientWorker], socket, master)
}
/**************************************************************************************************************************************************************/
class ClientMaster(client: Host) extends Actor {
  val socket = new DatagramSocket(client.port)
  val poolSize = 4
  val system = ActorSystem("ClientMaster")
  val router = system.actorOf(RoundRobinPool(poolSize).props(ClientWorker(socket, self)), name = "ClientSlave")

  def receive = {
    case Start(d) =>
      try {
        while (true) {
          val receivePacket = new DatagramPacket(new Array[Byte](client.maxDataSize), client.maxDataSize)
          socket.receive(receivePacket)
          router ! ReceiveFromServer(receivePacket)
        }
      } catch {
        case e: SocketException =>
          println("client socket is being closed...")
          socket.close()
          system.terminate()
      }
  }
}

/*************************************************************************************************************************************************************/
object Client {
  @throws[Exception]
  def main(args: Array[String]) {
    val client = new Host(port = 9875)

    val system = ActorSystem("Client")
    val masterActor = system.actorOf(Props(classOf[ClientMaster], client), name = "ClientMaster")

    println("Client listening on port 9875...")
    masterActor ! Start(new Date())
  }
}