package transport.udp

/**
  * Created by taner.gokalp on 14/06/16.
  */

case class WaitingUDPPacket(timestamp: Long, data: Any)
case class UDPPacket(seq: Int, ack: Int, data: Any)