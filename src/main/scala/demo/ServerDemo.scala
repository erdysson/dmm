package demo

import java.net.InetAddress

import akka.actor.ActorSystem
import application.host.Server
import application.matrix.Matrix
import application.tasks.CompletedTask
import messages.Process
import transport.rudp.{RUDPData, UDPChannel}

/**
  * Created by taner.gokalp on 14/06/16.
  */

object ServerDemo {
  @throws[Exception]
  def main(args: Array[String]) {
    val matrix = Matrix.random(40, withLogging = true)
    val transpose = Matrix.transpose(matrix, withLogging = true)
    val taskList = Matrix.distribute(matrix, transpose)

    // val config = (InetAddress.getByName("localhost"), 9876)
    // val udpChannel = UDPChannel(config._1, config._2, 1024)
    // val masterSystem = ActorSystem("Server")
    // val masterActor = masterSystem.actorOf(Server("WorkerPool", udpChannel, 4))
    // masterActor ! Process(taskList.toList)

    val remoteConfig = (InetAddress.getByName("localhost"), 9875)
    val channel = new UDPChannel(port = 9876, mtu = 4096, timeout = 3000)
    println(s"Server started at 9876...")
    Thread.sleep(1000)
    println("Computation is started...")
    for (t <- taskList) {
      channel.send(new RUDPData(t, remoteConfig._1, remoteConfig._2))
      val received = channel.receive()
      received.isEmpty match {
        case false =>
          val data = received.get
          println(s"Server received data : $data")
        case _ =>
      }
    }

    Thread.sleep(5000)
    println("Computation finished...")
  }

}
