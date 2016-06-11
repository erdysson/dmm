package messages

import java.net.{InetAddress, DatagramPacket}
import java.util.Date
import tasks._

/**
  * Created by taner.gokalp on 06/06/16.
  */

case class ReceivePacket(d: DatagramPacket)
case class SendPacket(wt: WorkerTask, address: InetAddress, port: Int)

// todo : maybe - merge messages later
// server side messages
case class SendToClient(task: Task, address: InetAddress, port: Int)
case class ReceiveFromClient(packet: DatagramPacket)
case class Result(completedTask: CompletedTask)
// client side messages
case class ReceiveFromServer(packet: DatagramPacket)
case class SendToServer(completedTask: CompletedTask, address: InetAddress, port: Int)
// common messages
case class Terminate(d: Date)
case class Start(d: Date)
