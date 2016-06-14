package messages

import java.util.Date

/**
  * Created by taner.gokalp on 14/06/16.
  */

/********** master messages **********/
case class Process(taskList: List[Any])
case class Listen()
case class Finalize()

/********** worker messages **********/
case class WakeUp(now: Date)
case class Sleep(now: Date)
case class CheckStatus()