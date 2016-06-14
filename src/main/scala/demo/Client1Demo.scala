package demo

import java.net.InetAddress

import akka.actor.ActorSystem
import application.tasks.Task
import transport.udp.UDPMaster
import transport.udp.channel.UDPChannel

/**
  * Created by taner.gokalp on 14/06/16.
  */
object Client1Demo {
  val config = (InetAddress.getLocalHost, 9875)
  val udpChannel = UDPChannel(config._1, config._2, 4096)

  val masterSystem = ActorSystem("Client1")
  val masterActor = masterSystem.actorOf(UDPMaster[Task]("WorkerPool", udpChannel))

  @throws[Exception]
  def main(args: Array[String]) {

  }
}

