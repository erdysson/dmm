package transport.udp

import java.net.InetAddress

/**
  * Created by taner.gokalp on 14/06/16.
  */

case class WaitingUDPPacket(timestamp: Long, data: Any, remoteConfig: (InetAddress, Int))
case class UDPPacket(seq: Int, ack: Int, data: Any)