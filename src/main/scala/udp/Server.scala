package udp

import java.io._
import java.net._
import akka.actor.{ActorSystem, Actor}
import matrix.Matrix
import tasks.{CompletedTask, Task}

import scala.collection.SortedMap
import scala.collection.mutable.ListBuffer

/**
  * Created by valhalla on 07/06/16.
  */

/*
  private final static Config akkaConfig = ConfigFactory.parseString(

            "my-dispatcher.type = BalancingDispatcher \n" +
            "my-dispatcher.executor = fork-join-executor \n" +
            "my-dispatcher.fork-join-executor.parallelism-min = 8 \n" +
            "my-dispatcher.fork-join-executor.parallelism-factor = 3.0 \n" +
            "my-dispatcher.fork-join-executor.parallelism-max = 64 "
);
   */

class Server(server: Host, clientList: List[Host], system: ActorSystem) extends Actor with Serializer {
  private val socket = new DatagramSocket(server.port)

  val matrix = Matrix.random(50, withLogging = true)
  val transposedMatrix = Matrix.transpose(matrix, withLogging = true)
  var distribution = Matrix.distribute(matrix, transposedMatrix, withLogging = false)
  var results = ListBuffer.empty[CompletedTask]

  def listen(): Unit = {
    while (distribution.nonEmpty) {
      val sendData = serializer(distribution.head)
      distribution = distribution.tail

      val sendPacket = new DatagramPacket(sendData, sendData.length, clientList.head.address, clientList.head.port)
      socket.send(sendPacket)

      val receivePacket = new DatagramPacket(new Array[Byte](server.maxDataSize), server.maxDataSize)
      socket.receive(receivePacket)

      val completedTask = deserializer(receivePacket.getData).asInstanceOf[CompletedTask]
      println(s"[Server] - received : ${completedTask.toString}")
      results += completedTask
    }
    socket.close()
    println("server socket is being closed...")
    println("result matrix is being calculated... please wait")
    val resultMatrix = SortedMap(results.groupBy(_.seqGroup).toSeq:_*).mapValues(cj => cj.sortBy(_.seq).toList.map(c => c.result)).values.toList
    Matrix.print(resultMatrix)

    system.terminate()
  }

  def receive = {
    case "start" => listen()
  }
}