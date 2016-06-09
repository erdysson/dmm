package udp

import java.io._
import java.net.{SocketException, DatagramPacket, InetAddress, DatagramSocket}
import akka.actor._
import akka.routing.RoundRobinGroup
import tasks.{WorkerTask, CompletedTask, Task}

/**
  * Created by valhalla on 07/06/16.
  */

trait Serializer {
  def serializer(task: AnyRef): Array[Byte] = {
    val outputStream = new ByteArrayOutputStream()
    var objectOutput: Option[ObjectOutput] = None

    try {
      objectOutput = Some(new ObjectOutputStream(outputStream))
      objectOutput.get.writeObject(task)
      outputStream.toByteArray
    } finally {
      try {
        if (objectOutput.nonEmpty)
          objectOutput.get.close()
      } catch {
        case e: IOException =>
          e.printStackTrace()
          Array.empty[Byte]
      }

      try {
        outputStream.close()
      } catch {
        case e: IOException =>
          e.printStackTrace()
          Array.empty[Byte]
      }
    }
  }

  def deserializer(serialized: Array[Byte]): AnyRef = {
    val inputStream = new ByteArrayInputStream(serialized)
    var objectInput: Option[ObjectInput] = None

    try {
      objectInput = Some(new ObjectInputStream(inputStream))
      val task = objectInput.get.readObject()
      task
    } finally {
      try {
        inputStream.close()
      } catch {
        case e: IOException =>
          e.printStackTrace()
          Array.empty[Byte]
      }

      try {
        if (objectInput.nonEmpty)
          objectInput.get.close()
      } catch {
        case e: IOException =>
          e.printStackTrace()
          Array.empty[Byte]
      }
    }
  }
}

case class ReceivePacket(d: DatagramPacket)
case class SendPacket(wt: WorkerTask, address: InetAddress, port: Int)

case class Host(val address: InetAddress = InetAddress.getLocalHost, val port: Int, maxDataSize: Int = 4096)

// todo : separate receiver and sender groups maybe ?
class Worker(name: String, group: String, socket: DatagramSocket) extends Actor with Serializer {
  def receive = {
    case p: ReceivePacket =>
      val serverMessage = deserializer(p.d.getData)

      serverMessage.isInstanceOf[Task] match {
        case true =>
          val message = serverMessage.asInstanceOf[Task]
          println(s"[$group $name] received => Seq: ${message.seq}, SeqGroup: ${message.seqGroup}")
          self ! SendPacket(message.complete, p.d.getAddress, p.d.getPort)

        case _ =>
          val message = serverMessage.asInstanceOf[CompletedTask]
          println(s"[$group $name] received => Seq: ${message.seq}, SeqGroup: ${message.seqGroup}")
          self ! SendPacket(message, p.d.getAddress, p.d.getPort)
      }

    case p: SendPacket =>
      try {
        p.wt.isInstanceOf[CompletedTask] match {
          case true =>
            val ct = p.wt.asInstanceOf[CompletedTask]
            println(s"[$group $name] sending => Seq: ${ct.seq}, SeqGroup: ${ct.seqGroup}")
            val reply = serializer(ct)
            socket.send(new DatagramPacket(reply, reply.length, p.address, p.port))

          case _ =>
            val t = p.wt.asInstanceOf[Task]
            println(s"[$group $name] sending => Seq: ${t.seq}, SeqGroup: ${t.seqGroup}")
            val reply = serializer(t)
            socket.send(new DatagramPacket(reply, reply.length, p.address, p.port))
        }
      } catch {
        case e: SocketException =>
          println(s"exception in sender [$group $name]")
          self ! PoisonPill
      }
  }
}

object Client {
  val system = ActorSystem("Client")
  val client = new Host(port = 9875)
  val socket = new DatagramSocket(client.port)

  val workers = List.tabulate(4)(i => system.actorOf(Props(classOf[Worker], "Worker" + i, "Client", socket), name = "Worker" + i))
  val router = system.actorOf(Props.empty.withRouter(RoundRobinGroup(workers.map(_.path.toString))))

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