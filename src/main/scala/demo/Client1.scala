package demo

import java.net.InetAddress

import akka.actor.ActorSystem
import application.tasks.Task
import transport.udp.{UDPChannel, UDPMaster}

/**
  * Created by taner.gokalp on 14/06/16.
  */
object Client1 {
  val config = (InetAddress.getLocalHost, 9875)
  val udpChannel = UDPChannel(config._1, config._2, 4096)

  val masterSystem = ActorSystem("Client1")
  val masterActor = masterSystem.actorOf(UDPMaster[Task]("WorkerPool", udpChannel))

  @throws[Exception]
  def main(args: Array[String]) {

  }
}

