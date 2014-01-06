package edu.arizona.sista.processor.fastnlp

import org.maltparserx.MaltConsoleEngine
import scala.collection.mutable.ArrayBuffer
import java.io.File

/**
 * Evaluates a model produced by TrainMalt
 * User: mihais
 * Date: 1/5/14
 */
object EvaluateMalt {
  val OUT_FILE = "malt.out"

  def main(args:Array[String]) {
    if(args.length != 2) {
      println("Usage: edu.arizona.sista.processor.fastnlp.TrainMalt <model name> <testing file>")
      System.exit(1)
    }
    val modelName = args(0)
    val testFile = args(1)
    val outFile = OUT_FILE

    val engine = new MaltConsoleEngine
    engine.startEngine(mkArgs(modelName, testFile, outFile))

    val goldDeps = readDependencies(testFile)
    val sysDeps = readDependencies(outFile)
    assert(goldDeps.size == sysDeps.size)
    new File(OUT_FILE).delete()

    val (las, uas) = score(goldDeps, sysDeps)
    println(s"LAS = $las")
    println(s"UAS = $uas")
  }

  def score(goldDeps:Array[Dependency], sysDeps:Array[Dependency]):(Double, Double) = {
    var correctLabeled = 0
    var correctUnlabeled = 0
    for(i <- 0 until goldDeps.size) {
      val g = goldDeps(i)
      val s = sysDeps(i)
      if(g.head == s.head) {
        correctUnlabeled += 1
        if(g.label == s.label)
          correctLabeled += 1
      }
    }

    val las = correctLabeled.toDouble / goldDeps.size.toDouble
    val uas = correctUnlabeled.toDouble / goldDeps.size.toDouble
    (las, uas)
  }

  def readDependencies(fn:String):Array[Dependency] = {
    val deps = new ArrayBuffer[Dependency]()
    for(line <- io.Source.fromFile(fn).getLines()) {
      val content = line.trim
      if(content.length > 0) {
        val tokens = content.split("\\s+")
        if(tokens.size < 8)
          throw new RuntimeException(s"ERROR: invalid output line in file $fn: $line")
        val label = tokens(7)
        val head = tokens(6).toInt
        deps += new Dependency(label, head)
      }
    }
    deps.toArray
  }

  def mkArgs(modelName:String, testFile:String, outFile:String):Array[String] = {
    val args = new ArrayBuffer[String]()

    args += "-m"
    args += "parse"

    args += "-l"
    args += "liblinear"

    args += "-c"
    args += modelName

    args += "-a"
    args += "nivreeager"

    args += "-i"
    args += testFile

    args += "-o"
    args += outFile

    println("Using command line: " + args.toList)
    args.toArray
  }
}

class Dependency(val label:String, val head:Int)