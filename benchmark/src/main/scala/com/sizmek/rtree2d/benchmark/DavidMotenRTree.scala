package com.sizmek.rtree2d.benchmark

import java.util

import com.github.davidmoten.rtree._
import com.github.davidmoten.rtree.geometry._
import org.openjdk.jmh.annotations._

import collection.JavaConverters._
import scala.collection.breakOut

class DavidMotenRTree extends BenchmarkBase {
  private[benchmark] var rtreeEntries: Array[Entry[PointOfInterest, Rectangle]] = _
  private[benchmark] var rtree: RTree[PointOfInterest, Rectangle] = _
  private[benchmark] var entriesToAddOrRemove: Array[Entry[PointOfInterest, Rectangle]] = _
  private[this] var xys: Array[Float] = _
  private[this] var curr: Int = _

  @Setup
  def setup(): Unit = {
    val points = genPoints
    val eps = overlap / Math.sqrt(size).toFloat
    rtreeEntries = points.map(p => Entries.entry(p, Geometries.rectangle(p.x - eps, p.y - eps, p.x + eps, p.y + eps)))(breakOut)
    if (shuffle) doShuffle(rtreeEntries)
    rtree = RTree.minChildren(1).maxChildren(nodeCapacity).create(util.Arrays.asList(rtreeEntries:_*))
    doShuffle(points)
    xys = genRequests(points)
    curr = 0
    entriesToAddOrRemove = rtreeEntries.slice(0, (size * partToAddOrRemove).toInt)
  }

  @Benchmark
  def apply: RTree[PointOfInterest, Rectangle] =
    RTree.minChildren(1).maxChildren(nodeCapacity).create(util.Arrays.asList(rtreeEntries:_*))

  @Benchmark
  def entries: Seq[Entry[PointOfInterest, Rectangle]] = rtree.entries().toBlocking.toIterable.asScala.toSeq

  @Benchmark
  def searchByPoint: Seq[Entry[PointOfInterest, Rectangle]] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    val x = xys(i)
    val y = xys(i + 1)
    rtree.search(Geometries.rectangle(x, y, x, y)).toBlocking.toIterable.asScala.toSeq
  }

  @Benchmark
  def insert: RTree[PointOfInterest, Rectangle] =
    RTree.minChildren(1).maxChildren(nodeCapacity)
      .create(util.Arrays.asList((rtreeEntries ++ entriesToAddOrRemove).toArray:_*))

  @Benchmark
  def remove: RTree[PointOfInterest, Rectangle] =
    RTree.minChildren(1).maxChildren(nodeCapacity)
      .create(util.Arrays.asList(rtreeEntries.diff(entriesToAddOrRemove).toArray:_*))
}
