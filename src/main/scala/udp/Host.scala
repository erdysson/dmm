package udp

import java.net.InetAddress

/**
  * Created by taner.gokalp on 11/06/16.
  */

case class Host(val address: InetAddress = InetAddress.getLocalHost, val port: Int, maxDataSize: Int = 4096)