package demo

import java.net.InetAddress

import akka.actor.{Props, Actor, ActorSystem}
import akka.routing.RoundRobinPool
import application.matrix.Matrix
import application.tasks.CompletedTask
import transport.rudp.UDPChannel
import scala.collection.mutable.ListBuffer

/**
  * Created by taner.gokalp on 14/06/16.
  */

case class Send(data: Any)

// may be master
class ServerListener(name: String, channel: UDPChannel) extends Actor {
  private var results = ListBuffer.empty[CompletedTask]

  def active: Receive = {
    case "listen" =>
      println(s"[$name] : Listening ${channel.address}:${channel.port}...")
      while (true) {
        val mayBeRUDPData = channel.receive()
        mayBeRUDPData match {
          case Some(data) =>
            println(s"[$name] : Received completed task")
            results += data.data.asInstanceOf[CompletedTask]
          case _ =>
        }
      }

    case "sleep" =>
      context become inactive
  }

  def inactive: Receive = {
    case "wake up" =>
      context become active
  }

  def receive = active
}

class Sender(name: String, channel: UDPChannel, remoteConfig: (InetAddress, Int)) extends Actor {

  def active: Receive = {
    case "sleep" =>
      context become inactive

    case Send(data) =>
      println(s"Sender actor sending $data")
      channel.send(data, remoteConfig._1, remoteConfig._2)
  }

  def inactive: Receive = {
    case "wake up" =>
      context become active
  }

  def receive = active
}

object ServerDemo {
  @throws[Exception]
  def main(args: Array[String]) {
    val matrix = Matrix.random(2, withLogging = true)
    val transpose = Matrix.transpose(matrix, withLogging = true)
    val taskList = Matrix.distribute(matrix, transpose)

    val remoteConfig = (InetAddress.getByName("localhost"), 9875)
    val channel = new UDPChannel(port = 9876, mtu = 4096, timeout = 3000)
    println(s"Server started at 9876...")
    Thread.sleep(1000)

    val system = ActorSystem("Server")
    val router = system.actorOf(RoundRobinPool(4).props(Props(classOf[Sender], "Server Sender", channel, remoteConfig)), name = "Router")
    val listener = system.actorOf(Props(classOf[ServerListener], "Server Listener", channel), name = "ServerListenerActor")

    listener ! "listen"

    println("Calculation will be started in a second...")
    Thread.sleep(1000)


    for (t <- taskList) {
      router ! Send(t)
    }
  }

}
