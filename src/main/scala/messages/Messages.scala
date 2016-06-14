package messages

import java.util.Date

import transport.udp.UDPPacket

/**
  * Created by taner.gokalp on 14/06/16.
  */

/********** master messages **********/
case class Result(udpPacket: UDPPacket)
case class ReTransmit(dataList: List[Any])
case class Process(taskList: List[Any])
case class Finalize()

/********** worker messages **********/
/* behaviour based messages */
case class WakeUp(now: Date)
case class Sleep(now: Date)

/********** data exchange - validation based messages **********/
case class Send(data: Any)
case class Receive()
case class CheckStatus()