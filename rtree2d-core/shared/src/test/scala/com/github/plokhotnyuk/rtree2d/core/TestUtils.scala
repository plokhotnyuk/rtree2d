package com.github.plokhotnyuk.rtree2d.core

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
    d <- positiveFloatGen
    e <- entryGen
  } yield (d, e)
  val latLonEntryGen: Gen[RTreeEntry[Int]] = for {
    lat1 <- latGen
    lon1 <- lonGen
    lat2 <- latGen
    lon2 <- lonGen
  } yield RTreeEntry(min(lat1, lat2), min(lon1, lon2), max(lat1, lat2), max(lon1, lon2), lastId.getAndIncrement())
  val entryListGen: Gen[Seq[RTreeEntry[Int]]] = Gen.oneOf(0, 1, 10, 100, 1000).flatMap(n => Gen.listOfN(n, entryGen))
  val distanceEntryListGen: Gen[Seq[(Float, RTreeEntry[Int])]] =
    Gen.oneOf(0, 1, 10, 100, 1000).flatMap(n => Gen.listOfN(n, distanceEntryGen))

  implicit def orderingByName[A <: RTreeEntry[Int]]: Ordering[A] =
    Ordering.by(e => (e.minX, e.minY, e.maxX, e.maxY, e.value))

  def intersects[T](es: Seq[RTreeEntry[T]], x: Float, y: Float): Seq[RTreeEntry[T]] = intersects(es, x, y, x, y)

  def intersects[T](es: Seq[RTreeEntry[T]], minX: Float, minY: Float, maxX: Float, maxY: Float): Seq[RTreeEntry[T]] =
    es.filter(e => intersects(e, minX, minY, maxX, maxY))

  def intersects[T](e: RTree[T], x: Float, y: Float): Boolean = e.minX <= x && x <= e.maxX && e.minY <= y && y <= e.maxY

  def intersects[T](e: RTree[T], minX: Float, minY: Float, maxX: Float, maxY: Float): Boolean =
    e.minX <= maxX && minX <= e.maxX && e.minY <= maxY && minY <= e.maxY

  def euclideanDistance[T](x: Float, y: Float, t: RTree[T]): Float = {
    val dx = max(abs((t.minX + t.maxX) / 2 - x) - (t.maxX - t.minX) / 2, 0)
    val dy = max(abs((t.minY + t.maxY) / 2 - y) - (t.maxY - t.minY) / 2, 0)
    sqrt(dx * dx + dy * dy).toFloat
  }

  def alignedHorizontally[T](e: RTree[T], lat: Float, lon: Float): Boolean =
    e.minX <= lat && lat <= e.maxX && (lon < e.minY || e.maxY < lon)

  def alignedVertically[T](e: RTree[T], lat: Float, lon: Float): Boolean =
    e.minY <= lon && lon <= e.maxY && (lat < e.minX || e.maxX < lat)

  // https://en.wikipedia.org/wiki/Haversine_formula + https://en.wikipedia.org/wiki/Earth_radius#Mean_radius
  def greatCircleDistance(lat1: Float, lon1: Float, lat2: Float, lon2: Float, radius: Float = 6371.0088f): Float = {
    val shdy = sin((lon1 - lon2) * PI / 180 / 2)
    val shdx = sin((lat1 - lat2) * PI / 180 / 2)
    (asin(sqrt(cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * shdy * shdy + shdx * shdx)) * 2 * radius).toFloat
  }
}
