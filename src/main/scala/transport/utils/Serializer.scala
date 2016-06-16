package transport.utils

import java.io._

/**
  * Created by taner.gokalp on 13/06/16.
  */

trait Serializer {

  def serialize(data: AnyRef): Array[Byte] = {
    val outputStream = new ByteArrayOutputStream()
    var objectOutput: Option[ObjectOutput] = None

    try {
      objectOutput = Some(new ObjectOutputStream(outputStream))
      objectOutput.get.writeObject(data)
      outputStream.toByteArray
    } finally {
      try {
        if (objectOutput.nonEmpty)
          objectOutput.get.close()
      } catch {
        case e: IOException =>
          e.printStackTrace()
          Array.empty[Byte]
      }

      try {
        outputStream.close()
      } catch {
        case e: IOException =>
          e.printStackTrace()
          Array.empty[Byte]
      }
    }
  }

  def deserialize(serialized: Array[Byte]): Any = {
    val inputStream = new ByteArrayInputStream(serialized)
    var objectInput: Option[ObjectInput] = None

    try {
      objectInput = Some(new ObjectInputStream(inputStream))
      val task = objectInput.get.readObject()
      task
    } finally {
      try {
        inputStream.close()
      } catch {
        case e: IOException =>
          e.printStackTrace()
          Array.empty[Byte]
      }

      try {
        if (objectInput.nonEmpty)
          objectInput.get.close()
      } catch {
        case e: IOException =>
          e.printStackTrace()
          Array.empty[Byte]
      }
    }
  }
}