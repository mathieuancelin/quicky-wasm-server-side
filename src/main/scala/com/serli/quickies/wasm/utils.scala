package com.serli.quickies.wasm

import akka.util.ByteString
import org.wasmer.{Instance, Memory}

import java.nio.file.{Files, Path}

class WasmerEnv(instance: Instance, memory: Memory) {

  def input(input: Array[Byte]): Integer = {
    val len: java.lang.Integer = input.length
    val input_pointer = instance.exports.getFunction("allocate").apply(len).apply(0).asInstanceOf[Integer]
    val memoryBuffer = memory.buffer
    memoryBuffer.position(input_pointer)
    memoryBuffer.put(input)
    input_pointer
  }

  def execFunction(name: String, inputs: Seq[java.lang.Integer] = Seq.empty, _max: Option[Int] = None, dyn: Boolean = true): ByteString = {
    val output_pointer = instance.exports.getFunction(name).apply(inputs:_*).apply(0).asInstanceOf[Integer]
    val memoryBuffer = memory.buffer()
    val limit = memoryBuffer.limit()
    val max = _max.getOrElse(limit)
    if (dyn) {
      var output = ByteString.empty
      var i = output_pointer
      while (i < max) {
        val b = new Array[Byte](1)
        memoryBuffer.position(i)
        memoryBuffer.get(b)
        if (b(0) > 0) {
          output = output ++ ByteString(b(0))
        } else {
          i = max + 1
        }
        i += 1
      }
      output
    } else {
      val data = new Array[Byte](max)
      memoryBuffer.position(output_pointer)
      memoryBuffer.get(data)
      ByteString(data)
    }
  }
}

object Wasmer {

  def path[A](path: Path, pages: Int = 0)(f: WasmerEnv => A): A = script(Files.readAllBytes(path), pages)(f)

  def script[A](script: Array[Byte], pages: Int = 0)(f: WasmerEnv => A): A = {
    val instance = new Instance(script)
    try {
      val memory = instance.exports.getMemory("memory")
      memory.grow(pages) // basic mem is 32kb, each page is 32kb more
      val env = new WasmerEnv(instance, memory)
      f(env)
    } finally {
      instance.close()
    }
  }
}