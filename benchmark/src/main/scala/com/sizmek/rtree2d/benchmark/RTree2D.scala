package com.sizmek.rtree2d.benchmark

import com.sizmek.rtree2d.core._
import org.openjdk.jmh.annotations._

class RTree2D extends BenchmarkBase {
  import EuclideanPlane._

  private[benchmark] var rtreeEntries: Array[RTreeEntry[PointOfInterest]] = _
  private[benchmark] var rtree: RTree[PointOfInterest] = _
  private[benchmark] var entriesToAdd: Array[RTreeEntry[PointOfInterest]] = _
  private[benchmark] var entriesToRemove: Array[RTreeEntry[PointOfInterest]] = _
  private[this] var xys: Array[Float] = _
  private[this] var curr: Int = _
  private[this] var eps: Float = _
  private[this] var rectEps: Float = _

  @Setup
  def setup(): Unit = {
    val points = genPoints
    if (shuffle) doShuffle(points)
    val sizeX = Math.sqrt(size).toFloat
    eps = overlap / (sizeX * 2)
    rectEps = rectSize / (sizeX * 2)
    rtreeEntries = new Array[RTreeEntry[PointOfInterest]](points.length)
    var i = 0
    while (i < points.length) {
      val p = points(i)
      rtreeEntries(i) = RTreeEntry(p.x - eps, p.y - eps, p.x + eps, p.y + eps, p)
      i += 1
    }
    rtree = RTree(rtreeEntries, nodeCapacity)
    doShuffle(points)
    xys = genRequests(points)
    curr = 0
    if (!shuffle) rtreeEntries = rtree.entries.toArray
    entriesToAdd = java.util.Arrays.copyOf(rtreeEntries, (size * partToAddOrRemove).toInt)
    entriesToRemove = rtreeEntries.slice(size - (size * partToAddOrRemove).toInt, size)
  }

  @Benchmark
  def apply: RTree[PointOfInterest] = RTree(rtreeEntries, nodeCapacity)

  @Benchmark
  def entries: Seq[RTreeEntry[PointOfInterest]] = rtree.entries

  @Benchmark
  def nearest: Option[RTreeEntry[PointOfInterest]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.nearest(xys(i), xys(i + 1)).map(_._2)
  }

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
    val e = rectEps
    rtree.searchAll(x - e, y - e, x + e, y + e)
  }

  @Benchmark
  def update: RTree[PointOfInterest] = RTree.update(rtree, entriesToRemove, entriesToAdd, nodeCapacity)
}
