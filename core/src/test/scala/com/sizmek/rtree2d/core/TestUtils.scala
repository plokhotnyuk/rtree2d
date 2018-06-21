package com.sizmek.rtree2d.core

import java.lang.Math._
import java.util.concurrent.atomic.AtomicInteger

import org.scalacheck.Gen

object TestUtils {
  val lastId = new AtomicInteger
  val floatGen: Gen[Float] = Gen.choose[Float](-1000, 1000)
  val latGen: Gen[Float] = Gen.choose[Float](-90, 90)
  val lonGen: Gen[Float] = Gen.choose[Float](-180, 180)
  val positiveIntGen: Gen[Int] = Gen.choose[Int](0, 200)
  val positiveFloatGen: Gen[Float] = Gen.choose[Float](0, 200)
  val entryGen: Gen[RTreeEntry[Int]] = for {
    x <- floatGen
    y <- floatGen
    w <- positiveFloatGen
    h <- positiveFloatGen
  } yield RTreeEntry(x, y, x + w, y + h, lastId.getAndIncrement())
  val distanceEntryGen: Gen[(Float, RTreeEntry[Int])] = for {
    d <- floatGen
    e <- entryGen
  } yield (d, e)
  val latLonEntryGen: Gen[RTreeEntry[Int]] = for {
    x1 <- latGen
    y1 <- lonGen
    x2 <- latGen
    y2 <- lonGen
  } yield RTreeEntry(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2), lastId.getAndIncrement())
  val entryListGen: Gen[Seq[RTreeEntry[Int]]] = Gen.oneOf(0, 1, 10, 100, 1000).flatMap(n => Gen.listOfN(n, entryGen))
  val distanceEntryListGen: Gen[Seq[(Float, RTreeEntry[Int])]] =
    Gen.oneOf(0, 1, 10, 100, 1000).flatMap(n => Gen.listOfN(n, distanceEntryGen))

  implicit def orderingByName[A <: RTreeEntry[Int]]: Ordering[A] = Ordering.by(e => (e.x1, e.y1, e.x2, e.y2, e.value))

  def intersects[T](es: Seq[RTreeEntry[T]], x: Float, y: Float): Seq[RTreeEntry[T]] = intersects(es, x, y, x, y)

  def intersects[T](es: Seq[RTreeEntry[T]], x1: Float, y1: Float, x2: Float, y2: Float): Seq[RTreeEntry[T]] =
    es.filter(e => intersects(e, x1, y1, x2, y2))

  def intersects[T](e: RTree[T], x: Float, y: Float): Boolean = e.x1 <= x && x <= e.x2 && e.y1 <= y && y <= e.y2

  def intersects[T](e: RTree[T], x1: Float, y1: Float, x2: Float, y2: Float): Boolean =
    e.x1 <= x2 && x1 <= e.x2 && e.y1 <= y2 && y1 <= e.y2

  def euclideanDistance[T](x: Float, y: Float, t: RTree[T]): Float = {
    val dx = Math.max(Math.abs((t.x1 + t.x2) / 2 - x) - (t.x2 - t.x1) / 2, 0)
    val dy = Math.max(Math.abs((t.y1 + t.y2) / 2 - y) - (t.y2 - t.y1) / 2, 0)
    Math.sqrt(dx * dx + dy * dy).toFloat
  }

  def alignedHorizontally[T](e: RTree[T], lat: Float, lon: Float): Boolean =
    e.x1 <= lat && lat <= e.x2 && (lon < e.y1 || e.y2 < lon)

  def alignedVertically[T](e: RTree[T], lat: Float, lon: Float): Boolean =
    e.y1 <= lon && lon <= e.y2 && (lat < e.x1 || e.x2 < lat)

  // https://en.wikipedia.org/wiki/Haversine_formula + https://en.wikipedia.org/wiki/Earth_radius#Mean_radius
  def greatCircleDistance(lat1: Float, lon1: Float, lat2: Float, lon2: Float, radius: Float = 6371.0088f): Float = {
    val shdy = sin((lon1 - lon2) * PI / 180 / 2)
    val shdx = sin((lat1 - lat2) * PI / 180 / 2)
    (asin(sqrt(cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * shdy * shdy + shdx * shdx)) * 2 * radius).toFloat
  }
}
