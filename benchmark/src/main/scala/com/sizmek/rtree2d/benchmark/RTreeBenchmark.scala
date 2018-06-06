package com.sizmek.rtree2d.benchmark

import java.util.concurrent.TimeUnit

import com.sizmek.rtree2d.core._
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import scala.collection.breakOut

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1, jvmArgs = Array(
  "-server",
  "-Xms2g",
  "-Xmx2g",
  "-XX:NewSize=1g",
  "-XX:MaxNewSize=1g",
  "-XX:InitialCodeCacheSize=256m",
  "-XX:ReservedCodeCacheSize=256m",
  "-XX:+UseParallelGC",
  "-XX:-UseBiasedLocking",
  "-XX:+AlwaysPreTouch"
))
class RTreeBenchmark {
  private[benchmark] var rtreeEntries: Array[RTreeEntry[PointOfInterest]] = _
  private[benchmark] var rtree: RTree[PointOfInterest] = _
  private[this] var xys: Array[Float] = _
  private[this] var curr: Int = 0
  private[this] var blackhole: Blackhole = _
  private[this] val exit: RTreeEntry[PointOfInterest] => Boolean = x => {
    blackhole.consume(x)
    true
  }
  private[this] val continue: RTreeEntry[PointOfInterest] => Boolean = x =>  {
    blackhole.consume(x)
    false
  }

  val nodeCapacity = 16 // can be a param on demand

  @Param(Array("0.1", "10")) // number of overlaps in each of 4 directions (should be less than 0.5 for no overlap)
  var overlap = 0.1f

  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size = 10000

  @Param(Array("false", "true"))
  var shuffle = true

  @Setup
  def setup(): Unit = {
    val l = Math.sqrt(size).toFloat
    val li = l.toInt
    val points: Array[PointOfInterest] = (0 to size).map { i =>
      val x = (i / li) / l
      val y = (i % li) / l
      PointOfInterest(x, y)
    }(breakOut)
    val eps = overlap / l
    rtreeEntries = points.map(p => RTreeEntry(p.x - eps, p.y - eps, p.x + eps, p.y + eps, p))(breakOut)
    if (shuffle) doShuffle(rtreeEntries) // shuffle entries
    rtree = RTree(rtreeEntries, nodeCapacity)
    doShuffle(points) // shuffle points for requests
    xys = new Array[Float](size + size)
    var i = 0
    while (i < xys.length) {
      val p = points(i >> 1)
      xys(i) = p.x
      xys(i + 1) = p.y
      i += 2
    }
  }

  @Benchmark
  def apply: RTree[PointOfInterest] = RTree(rtreeEntries, nodeCapacity)

  @Benchmark
  def entries: Seq[RTreeEntry[PointOfInterest]] = rtree.entries

  @Benchmark
  def searchFirst(bh: Blackhole): Boolean = {
    blackhole = bh
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.search(xys(i), xys(i + 1))(exit)
  }

  @Benchmark
  def searchAll(bh: Blackhole): Boolean = {
    blackhole = bh
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.search(xys(i), xys(i + 1))(continue)
  }

  @Benchmark
  def searchAndCollectAll: Seq[RTreeEntry[PointOfInterest]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.searchAll(xys(i), xys(i + 1))
  }

  private[this] def doShuffle[A](as: Array[A]): Unit = {
    val rnd = new util.Random(0)
    var i = as.length - 1
    while (i > 0) {
      val i1 = rnd.nextInt(i)
      val a = as(i1)
      as(i1) = as(i)
      as(i) = a
      i -= 1
    }
  }
}

case class PointOfInterest(x: Float, y: Float)