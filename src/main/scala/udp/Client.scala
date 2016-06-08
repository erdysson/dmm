package udp

import java.io._
import java.net.{DatagramPacket, InetAddress, DatagramSocket}
import akka.actor.{ActorSystem, Actor}
import tasks.Task

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
      println("serialization completed...")
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
      println("deserialization completed...")
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

case class Host(val address: InetAddress = InetAddress.getLocalHost, val port: Int, maxDataSize: Int = 1024)

class Client(client: Host, system: ActorSystem) extends Actor with Serializer{
  val socket = new DatagramSocket(client.port)
  var dataCount = 0

  def listen(): Unit = {
    while (dataCount < 2500) {
      val receivePacket = new DatagramPacket(new Array[Byte](client.maxDataSize), client.maxDataSize)
      socket.receive(receivePacket)

      val serverMessage = deserializer(receivePacket.getData).asInstanceOf[Task]
      val (serverAddress, serverPort) = (receivePacket.getAddress, receivePacket.getPort)
      println(s"[Client] - received : ${serverMessage.toString}")
      dataCount += 1

      val reply = serializer(serverMessage.complete)
      val replyPacket = new DatagramPacket(reply, reply.length, serverAddress, serverPort)
      socket.send(replyPacket)
    }
    println("client socket is being closed...")
    socket.close()
    system.terminate()
  }

  def receive = {
    case "start" => listen()
  }
}
