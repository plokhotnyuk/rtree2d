package com.github.plokhotnyuk.rtree2d.benchmark

import java.util

import com.github.davidmoten.rtree2.Entries._
import com.github.davidmoten.rtree2._
import com.github.davidmoten.rtree2.geometry.Geometries._
import com.github.davidmoten.rtree2.geometry._
import org.openjdk.jmh.annotations._

import collection.JavaConverters._

class DavidMotenRTree extends BenchmarkBase {
  private[benchmark] var rtreeEntries: Array[Entry[PointOfInterest, Rectangle]] = _
  private[benchmark] var rtree: RTree[PointOfInterest, Rectangle] = _
  private[benchmark] var entriesToAdd: Array[Entry[PointOfInterest, Rectangle]] = _
  private[benchmark] var entriesToRemove: Array[Entry[PointOfInterest, Rectangle]] = _
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
    if (!shuffle) rtreeEntries = rtree.entries().asScala.toArray
    entriesToAdd = java.util.Arrays.copyOf(rtreeEntries, (size * partToUpdate).toInt)
    entriesToRemove = rtreeEntries.slice(size - (size * partToUpdate).toInt, size)
  }

  @Benchmark
  def apply: RTree[PointOfInterest, Rectangle] =
    RTree.minChildren(1).maxChildren(nodeCapacity).loadingFactor(1.0).create(util.Arrays.asList(rtreeEntries:_*))

  @Benchmark
  def entries: Seq[Entry[PointOfInterest, Rectangle]] = {
    val res = rtree.entries().asScala.toArray
    res
  }

  @Benchmark
  def nearest: Option[Entry[PointOfInterest, Rectangle]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.nearest(point(xys(i), xys(i + 1)), Double.PositiveInfinity, 1).asScala.headOption
  }

  @Benchmark
  def nearestK: Seq[Entry[PointOfInterest, Rectangle]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    val res = rtree.nearest(point(xys(i), xys(i + 1)), Double.PositiveInfinity, nearestMax).asScala.toArray
    res
  }

  @Benchmark
  def searchByPoint: Seq[Entry[PointOfInterest, Rectangle]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    val res = rtree.search(point(xys(i), xys(i + 1))).asScala.toArray
    res
  }

  @Benchmark
  def searchByRectangle: Seq[Entry[PointOfInterest, Rectangle]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    val x = xys(i)
    val y = xys(i + 1)
    val e = rectEps
    val res = rtree.search(rectangle(x - e, y - e, x + e, y + e)).asScala.toArray
    res
  }

  @Benchmark
  def update: RTree[PointOfInterest, Rectangle] = {
    val es = rtree.entries().asScala.toArray.diff(entriesToRemove) ++ entriesToAdd
    RTree.minChildren(1).maxChildren(nodeCapacity).loadingFactor(1.0).create(util.Arrays.asList(es:_*))
  }
}
