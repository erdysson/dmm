package actors

import java.net.{SocketException, DatagramPacket, DatagramSocket}
import akka.actor.{Props, Actor}
import common.Serializer
import messages.{SendPacket, ReceivePacket}
import tasks.{CompletedTask, Task}

/**
  * Created by taner.gokalp on 11/06/16.
  */

class Worker(socket: DatagramSocket) extends Actor with Serializer {
  def receive = {
    case p: ReceivePacket =>
      try {
        val incoming = deserializer(p.d.getData)
        incoming match {
          case t: Task =>
            println(s"[${self.path.toString}] received => Seq: ${t.seq}, SeqGroup: ${t.seqGroup}")
            self ! SendPacket(Task.complete(t), p.d.getAddress, p.d.getPort)

          case ct: CompletedTask =>
            println(s"[${self.path.toString}] received => Seq: ${ct.seq}, SeqGroup: ${ct.seqGroup}")
            self ! SendPacket(ct, p.d.getAddress, p.d.getPort)
        }
      }

    case p: SendPacket =>
      try {
        p.wt match {
          case CompletedTask(seq, seqGroup, result) =>
            println(s"[${self.path.toString}] sending => Seq: $seq, SeqGroup: $seqGroup")
            val reply = serializer(p.wt)
            socket.send(new DatagramPacket(reply, reply.length, p.address, p.port))

          case Task(seq, seqGroup, vector1, vector2) =>
            println(s"[${self.path.toString}] sending => Seq: $seq, SeqGroup: $seqGroup")
            val reply = serializer(p.wt)
            socket.send(new DatagramPacket(reply, reply.length, p.address, p.port))
        }
      } catch {
        case e: SocketException => println(s"exception in sender [${self.path.toString}]")
      }
  }
}

object Worker {
  def apply(socket: DatagramSocket): Props = Props(classOf[Worker], socket)
}
