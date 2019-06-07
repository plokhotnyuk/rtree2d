package com.github.plokhotnyuk.rtree2d.benchmark

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

case class PointOfInterest(x: Float, y: Float)

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = Array(
  "-server",
  "-Xms12g",
  "-Xmx12g",
  "-XX:NewSize=6g",
  "-XX:MaxNewSize=6g",
  "-XX:InitialCodeCacheSize=256m",
  "-XX:ReservedCodeCacheSize=256m",
  "-XX:+UseParallelOldGC",
  "-XX:-UseBiasedLocking",
  "-XX:+AlwaysPreTouch"
))
abstract class BenchmarkBase {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000", "10000000"))
  var size = 1000 // a number of entries in the R-tree

  @Param(Array("false", "true"))
  var shuffle = true // a flag to turn on/off shuffling of entries before R-tree building

  @Param(Array("1", "10"))
  var overlap = 1f // a size of entries relative to interval between them

  @Param(Array("10", "100"))
  var rectSize = 10.0f // a size of rectangle request relative to interval between points

  @Param(Array("10", "100"))
  var nearestMax = 10 // a maximum number of entries to return for nearest query

  @Param(Array("4", "16"))
  var nodeCapacity = 16 // a maximum number of children nodes (BEWARE: Archery use hard coded 50 for limiting a number of children nodes)

  @Param(Array("0.01", "0.1"))
  var partToUpdate = 0.1f // a part of RTree to update

  @Param(Array("plane", "spherical"))
  var geometry = "plane" // to switch geometry between `plane` and `spherical` (currently available only for the RTree2D library)

  def doShuffle[A](as: Array[A]): Unit = {
    val rnd = new util.Random(7777777)
    var i = as.length - 1
    while (i > 0) {
      val i1 = rnd.nextInt(i)
      val a = as(i1)
      as(i1) = as(i)
      as(i) = a
      i -= 1
    }
  }

  def genPoints: Array[PointOfInterest] = {
    val l = Math.sqrt(size).toFloat
    val li = l.toInt
    val points = new Array[PointOfInterest](size)
    var i = 0
    while (i < size) {
      val x = (i / li) / l
      val y = (i % li) / l
      points(i) = PointOfInterest(x, y)
      i += 1
    }
    points
  }

  def genRequests(points: Array[PointOfInterest]): Array[Float] = {
    val xys = new Array[Float](2 * size)
    var i = 0
    while (i < xys.length) {
      val p = points(i >> 1)
      xys(i) = p.x
      xys(i + 1) = p.y
      i += 2
    }
    xys
  }
}
