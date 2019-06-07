package com.github.plokhotnyuk.rtree2d.benchmark

import java.util

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.index.strtree._
import org.openjdk.jmh.annotations._

import scala.collection.mutable.ArrayBuffer

class JTS extends BenchmarkBase {
  private[benchmark] var rtree: STRtreeWrapper = _
  private[benchmark] var rtreeEntries: Array[(Envelope, PointOfInterest)] = _
  private[benchmark] var entriesToAdd: Array[(Envelope, PointOfInterest)] = _
  private[benchmark] var entriesToRemove: Array[(Envelope, PointOfInterest)] = _
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
    rtreeEntries = new Array[(Envelope, PointOfInterest)](points.length)
    var i = 0
    while (i < points.length) {
      val p = points(i)
      rtreeEntries(i) = (new Envelope(p.x - eps, p.x + eps, p.y - eps, p.y + eps), p)
      i += 1
    }
    rtree = STRtreeWrapper(nodeCapacity, rtreeEntries)
    doShuffle(points)
    xys = genRequests(points)
    curr = 0
    if (!shuffle) rtreeEntries = rtree.entries.toArray
    entriesToAdd = java.util.Arrays.copyOf(rtreeEntries, (size * partToUpdate).toInt)
    entriesToRemove = rtreeEntries.slice(size - (size * partToUpdate).toInt, size)
  }

  @Benchmark
  def apply: STRtreeWrapper = STRtreeWrapper(nodeCapacity, rtreeEntries)

  @Benchmark
  def entries: Seq[(Envelope, PointOfInterest)] = rtree.entries

  @Benchmark
  def nearest: Option[(Envelope, PointOfInterest)] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.nearest(xys(i), xys(i + 1))
  }

  @Benchmark
  def nearestK: Seq[(Envelope, PointOfInterest)] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    rtree.nearestK(xys(i), xys(i + 1), nearestMax)
  }

  @Benchmark
  def searchByPoint: Seq[(Envelope, PointOfInterest)] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    val x = xys(i)
    val y = xys(i + 1)
    rtree.search(x, y, x, y)
  }

  @Benchmark
  def searchByRectangle: Seq[(Envelope, PointOfInterest)] = {
    val i = curr
    curr = if (i + 2 < xys.length) i + 2 else 0
    val x = xys(i)
    val y = xys(i + 1)
    val e = rectEps
    rtree.search(x - e, y - e, x + e, y + e)
  }

  @Benchmark
  def update: STRtreeWrapper = STRtreeWrapper(nodeCapacity, rtree.entries.diff(entriesToRemove) ++ entriesToAdd)
}

case class STRtreeWrapper(nodeCapacity: Int, entries: Seq[(Envelope, PointOfInterest)]) {
  private[this] val strTree = new STRtree(nodeCapacity) {
    entries.foreach(e => insert(e._1, e))
    build()
  }
  private[this] val itemDistance = new ItemDistance {
    override def distance(item1: ItemBoundable, item2: ItemBoundable): Double =
      item1.getBounds.asInstanceOf[Envelope].distance(item2.getBounds.asInstanceOf[Envelope])
  }

  def search(minX: Float, minY: Float, maxX: Float, maxY: Float): Seq[(Envelope, PointOfInterest)] = {
    val res = new ArrayBuffer[(Envelope, PointOfInterest)]
    strTree.query(new Envelope(minX, maxX, minY, maxY), new ItemVisitor {
      override def visitItem(item: scala.Any): Unit = res += item.asInstanceOf[(Envelope, PointOfInterest)]
    })
    res
  }

  def nearest(x: Float, y: Float): Option[(Envelope, PointOfInterest)] =
    Option(strTree.nearestNeighbour(new Envelope(x, x, y, y), null, itemDistance).asInstanceOf[(Envelope, PointOfInterest)])

  def nearestK(x: Float, y: Float, k: Int): Seq[(Envelope, PointOfInterest)] = {
    val res = strTree.nearestNeighbour(new Envelope(x, x, y, y), null, itemDistance, k)
    util.Arrays.copyOf(res, res.length, classOf[Array[(Envelope, PointOfInterest)]])
  }
}
