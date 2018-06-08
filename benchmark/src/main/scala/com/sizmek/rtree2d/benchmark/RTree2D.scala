package com.sizmek.rtree2d.benchmark

import com.sizmek.rtree2d.core._
import org.openjdk.jmh.annotations._

import scala.collection.breakOut

class RTree2D extends BenchmarkBase {
  private[benchmark] var rtreeEntries: Array[RTreeEntry[PointOfInterest]] = _
  private[benchmark] var rtree: RTree[PointOfInterest] = _
  private[benchmark] var entriesToAddOrRemove: Array[RTreeEntry[PointOfInterest]] = _
  private[this] var xys: Array[Float] = _
  private[this] var curr: Int = _
  private[this] var eps: Float = _

  @Setup
  def setup(): Unit = {
    val points = genPoints
    eps = overlap / Math.sqrt(size).toFloat
    rtreeEntries = points.map(p => RTreeEntry(p.x - eps, p.y - eps, p.x + eps, p.y + eps, p))(breakOut)
    if (shuffle) doShuffle(rtreeEntries)
    rtree = RTree(rtreeEntries, nodeCapacity)
    doShuffle(points)
    xys = genRequests(points)
    curr = 0
    entriesToAddOrRemove = rtreeEntries.slice(0, (size * partToAddOrRemove).toInt)
  }

  @Benchmark
  def apply: RTree[PointOfInterest] = RTree(rtreeEntries, nodeCapacity)

  @Benchmark
  def entries: Seq[RTreeEntry[PointOfInterest]] = rtree.entries

  @Benchmark
  def searchByPoint: Seq[RTreeEntry[PointOfInterest]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.searchAll(xys(i), xys(i + 1))
  }

  @Benchmark
  def searchByRectangle: Seq[RTreeEntry[PointOfInterest]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    val x = xys(i)
    val y = xys(i + 1)
    val e = eps
    rtree.searchAll(x - e, y - e, x + e, y + e)
  }

  @Benchmark
  def insert: RTree[PointOfInterest] = RTree(rtreeEntries ++ entriesToAddOrRemove, nodeCapacity)

  @Benchmark
  def remove: RTree[PointOfInterest] = RTree(rtreeEntries.diff(entriesToAddOrRemove), nodeCapacity)
}
