package com.sizmek.rtree2d.benchmark

import archery._
import org.openjdk.jmh.annotations._

import scala.collection.breakOut

class Archery extends BenchmarkBase {
  private[benchmark] var rtree: RTree[PointOfInterest] = _
  private[benchmark] var rtreeEntries: Array[Entry[PointOfInterest]] = _
  private[benchmark] var entriesToAddOrRemove: Array[Entry[PointOfInterest]] = _
  private[this] var xys: Array[Float] = _
  private[this] var curr: Int = _
  private[this] var eps: Float = _

  @Setup
  def setup(): Unit = {
    val points = genPoints
    eps = overlap / Math.sqrt(size).toFloat
    rtreeEntries = points.map(p => Entry(Box(p.x - eps, p.y - eps, p.x + eps, p.y + eps), p))(breakOut)
    if (shuffle) doShuffle(rtreeEntries)
    rtree = RTree(rtreeEntries:_*)
    doShuffle(points)
    xys = genRequests(points)
    curr = 0
    entriesToAddOrRemove = rtreeEntries.slice(0, (size * partToAddOrRemove).toInt)
  }

  @Benchmark
  def apply: RTree[PointOfInterest] = RTree(rtreeEntries:_*)

  @Benchmark
  def entries: Seq[Entry[PointOfInterest]] = rtree.entries.toSeq

  @Benchmark
  def searchByPoint: Seq[Entry[PointOfInterest]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.searchIntersection(Point(xys(i), xys(i + 1)))
  }

  @Benchmark
  def searchByRectangle: Seq[Entry[PointOfInterest]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    val x = xys(i)
    val y = xys(i + 1)
    val e = eps
    rtree.searchIntersection(Box(x - e, y - e, x + e, y + e))
  }

  @Benchmark
  def insert: RTree[PointOfInterest] = rtree.insertAll(entriesToAddOrRemove)

  @Benchmark
  def remove: RTree[PointOfInterest] = rtree.removeAll(entriesToAddOrRemove)
}
