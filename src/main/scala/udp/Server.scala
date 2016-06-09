package udp

import java.net._
import akka.actor.{Props, ActorSystem, Actor}
import akka.routing.{BalancingPool, RoundRobinPool, RoundRobinGroup}
import matrix.Matrix
import tasks.{CompletedTask}
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


/*
  - todo : client distributor router actor system -> in main
  - todo : result calculator router actor system -> self calculating
  - todo : ack listener -> distributor
 */

object Server extends Serializer {
  val system = ActorSystem("Server")
  val server = new Host(port = 9876)
  val socket = new DatagramSocket(server.port)

  val workers = List.tabulate(4)(i => system.actorOf(Props(classOf[Worker], "Worker" + i, "Server", socket), name = "Server" + i))
  // todo : do not use round robin group !
  val router = system.actorOf(Props.empty.withRouter(RoundRobinGroup(workers.map(_.path.toString))))
  val router2 = system.actorOf(RoundRobinPool(4).props(Props(classOf[Worker], s"Worker", "Server", socket)), "router")
  val router3 = system.actorOf(BalancingPool(4).props(Props(classOf[Worker], s"Worker", "Server", socket)), "router")

  @throws[Exception]
  def main(args: Array[String]) {
    // todo : remove
    val (address, port) = (InetAddress.getLocalHost, 9875)

    val matrix = Matrix.random(4, withLogging = false)
    val transposedMatrix = Matrix.transpose(matrix, withLogging = false)
    val distribution = Matrix.distribute(matrix, transposedMatrix, withLogging = false)
    var results = ListBuffer.empty[CompletedTask]

    val clients = List.tabulate(3)(i => (InetAddress.getLocalHost, 9875 - i))

    println("server started at port 9875...")
    println("distribution will be started in 1 second...")
    Thread.sleep(1000)

    try {
      for (i <- distribution.indices) {
        // val sendData = serializer(distribution(i))
        // println(s"sent data size : ${sendData.size}")

        val clientIndex = 0 // i % clients.size

        router ! SendPacket(distribution(i), address, port)
        // val sendPacket = new DatagramPacket(sendData, sendData.length, clients(i)._1, clients(i)._2)
        // val sendPacket = new DatagramPacket(sendData, sendData.length, address, port)
        // socket.send(sendPacket)

        val receivePacket = new DatagramPacket(new Array[Byte](server.maxDataSize), server.maxDataSize)
        socket.receive(receivePacket)

        router ! ReceivePacket(receivePacket)

        //val completedTask = deserializer(receivePacket.getData).asInstanceOf[CompletedTask]
        //println(s"[Server] - received => Seq: ${completedTask.seq}, SeqGroup: ${completedTask.seqGroup}")
        // results += completedTask
      }
      //socket.close()
      println(s"server socket is being closed... result size : ${results.size}")
      println("result matrix is being calculated... please wait")
      Thread.sleep(1000)
      //val resultMatrix = SortedMap(results.groupBy(_.seqGroup).toSeq:_*).mapValues(cj => cj.sortBy(_.seq).toList.map(c => c.result)).values.toList
      //Matrix.print(resultMatrix)
      //system.terminate()
    } catch {
      case e: SocketException => e.printStackTrace()
    }
  }
}