package com.sizmek.rtree2d.benchmark

import java.util

import com.github.davidmoten.rtree.Entries._
import com.github.davidmoten.rtree._
import com.github.davidmoten.rtree.geometry.Geometries._
import com.github.davidmoten.rtree.geometry._
import org.openjdk.jmh.annotations._

import collection.JavaConverters._

class DavidMotenRTree extends BenchmarkBase {
  private[benchmark] var rtreeEntries: Array[Entry[PointOfInterest, Rectangle]] = _
  private[benchmark] var rtree: RTree[PointOfInterest, Rectangle] = _
  private[benchmark] var entriesToAddOrRemove: Array[Entry[PointOfInterest, Rectangle]] = _
  private[this] var xys: Array[Float] = _
  private[this] var curr: Int = _
  private[this] var eps: Float = _

  @Setup
  def setup(): Unit = {
    val points = genPoints
    if (shuffle) doShuffle(points)
    eps = overlap / Math.sqrt(size).toFloat
    rtreeEntries = new Array[Entry[PointOfInterest, Rectangle]](points.length)
    var i = 0
    while (i < points.length) {
      val p = points(i)
      rtreeEntries(i) = entry(p, rectangle(p.x - eps, p.y - eps, p.x + eps, p.y + eps))
      i += 1
    }
    rtree = RTree.minChildren(1).maxChildren(nodeCapacity).loadingFactor(1.0).create(util.Arrays.asList(rtreeEntries:_*))
    doShuffle(points)
    xys = genRequests(points)
    curr = 0
    if (!shuffle) rtreeEntries = rtree.entries().toBlocking.toIterable.asScala.toArray
    entriesToAddOrRemove = java.util.Arrays.copyOf(rtreeEntries, (size * partToAddOrRemove).toInt)
  }

  @Benchmark
  def apply: RTree[PointOfInterest, Rectangle] =
    RTree.minChildren(1).maxChildren(nodeCapacity).loadingFactor(1.0).create(util.Arrays.asList(rtreeEntries:_*))

  @Benchmark
  def entries: Seq[Entry[PointOfInterest, Rectangle]] = rtree.entries().toBlocking.toIterable.asScala.toSeq

  @Benchmark
  def searchByPoint: Seq[Entry[PointOfInterest, Rectangle]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.search(point(xys(i), xys(i + 1))).toBlocking.toIterable.asScala.toSeq
  }

  @Benchmark
  def searchByRectangle: Seq[Entry[PointOfInterest, Rectangle]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    val x = xys(i)
    val y = xys(i + 1)
    val e = eps
    rtree.search(rectangle(x - e, y - e, x + e, y + e)).toBlocking.toIterable.asScala.toSeq
  }

  @Benchmark
  def insert: RTree[PointOfInterest, Rectangle] =
    RTree.minChildren(1).maxChildren(nodeCapacity).loadingFactor(1.0)
      .create(util.Arrays.asList((rtreeEntries ++ entriesToAddOrRemove).toArray:_*))

  @Benchmark
  def remove: RTree[PointOfInterest, Rectangle] =
    RTree.minChildren(1).maxChildren(nodeCapacity).loadingFactor(1.0)
      .create(util.Arrays.asList(rtreeEntries.diff(entriesToAddOrRemove).toArray:_*))
}
