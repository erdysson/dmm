package transport.udp.worker

import akka.actor.{Actor, ActorRef}
import transport.udp.channel.UDPChannel

/**
  * Created by taner.gokalp on 15/06/16.
  */
class ClientWorker(val uDPChannel: UDPChannel, master: ActorRef) extends Actor {

}
