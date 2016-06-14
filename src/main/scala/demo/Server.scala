package demo

import java.net.InetAddress

import akka.actor.ActorSystem
import application.matrix.Matrix
import application.tasks.CompletedTask
import transport.udp.{Process, UDPChannel, UDPMaster}

/**
  * Created by taner.gokalp on 14/06/16.
  */

object Server {
  val matrix = Matrix.random(10, withLogging = true)
  val transpose = Matrix.transpose(matrix, withLogging = true)

  val taskList = Matrix.distribute(matrix, transpose)

  val config = (InetAddress.getLocalHost, 9876)
  val udpChannel = UDPChannel(config._1, config._2, 4096)

  val masterSystem = ActorSystem("Server")
  val masterActor = masterSystem.actorOf(UDPMaster[CompletedTask]("WorkerPool", udpChannel))

  @throws[Exception]
  def main(args: Array[String]) {
    println(s"Server started at ${config._1}:${config._2}...")
    Thread.sleep(1000)
    println("Computation is started...")
    masterActor ! Process(taskList.toList)
  }

}
