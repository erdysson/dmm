package messages

import java.net.{InetAddress, DatagramPacket}

import tasks._

/**
  * Created by taner.gokalp on 06/06/16.
  */

case class ReceivePacket(d: DatagramPacket)
case class SendPacket(wt: WorkerTask, address: InetAddress, port: Int)
