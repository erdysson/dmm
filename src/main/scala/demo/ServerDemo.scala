package demo

import java.net.InetAddress

import akka.actor.ActorSystem
import application.host.Server
import application.matrix.Matrix
import application.tasks.CompletedTask
import messages.Process
import transport.udp.UDPChannel

/**
  * Created by taner.gokalp on 14/06/16.
  */

object ServerDemo {
  val matrix = Matrix.random(10, withLogging = true)
  val transpose = Matrix.transpose(matrix, withLogging = true)

  val taskList = Matrix.distribute(matrix, transpose)

  val config = (InetAddress.getLocalHost, 9876)
  val udpChannel = UDPChannel(config._1, config._2, 4096)

  val masterSystem = ActorSystem("Server")
  val masterActor = masterSystem.actorOf(Server[CompletedTask]("WorkerPool", udpChannel))

  @throws[Exception]
  def main(args: Array[String]) {
    println(s"Server started at ${config._1}:${config._2}...")
    Thread.sleep(1000)
    println("Computation is started...")
    masterActor ! Process(taskList.toList)
  }

}
