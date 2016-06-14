package demo

import java.net.InetAddress

import akka.actor.ActorSystem
import application.tasks.Task
import messages.Listen
import transport.udp.channel.UDPChannel
import application.host.Client

/**
  * Created by taner.gokalp on 14/06/16.
  */
object Client1Demo {
  val config = (InetAddress.getLocalHost, 9875)
  val udpChannel = UDPChannel(config._1, config._2, 3048)

  val masterSystem = ActorSystem("Client1")
  val masterActor = masterSystem.actorOf(Client[Task]("WorkerPool", udpChannel, 4))

  @throws[Exception]
  def main(args: Array[String]) {
    println(s"Client started at ${config._1}:${config._2}...")
    Thread.sleep(1000)
    masterActor ! Listen()
  }
}

