package com.sizmek.rtree2d.core

import com.sizmek.rtree2d.core.TestUtils._
import org.scalacheck.Gen
import org.scalacheck.Prop._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.prop.Checkers

class GeoRTreeCheckers extends WordSpec with Checkers {
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100, maxDiscardedFactor = 100)

  implicit def orderingByName[A <: GeoRTreeEntry[Int]]: Ordering[A] =
    Ordering.by(e => (e.minLat, e.minLon, e.maxLat, e.maxLon, e.value, e.sinMinLat, e.cosMinLat, e.sinMaxLat, e.cosMaxLat))

  "GeoRTree" when {
    "asked to calculate distance from point to an RTree" should {
      "return 0 if the point is inside it" in check {
        forAll(latLonEntryGen, Gen.choose[Float](0, 1), Gen.choose[Float](0, 1)) {
          (t: GeoRTreeEntry[Int], rdx: Float, rdy: Float) =>
            val lat = t.minLat + rdx * (t.maxLat - t.minLat)
            val lon = t.minLon + rdy * (t.maxLon - t.minLon)
            propBoolean(geoIntersects(t, lat, lon)) ==> {
              val radLat = lat * GeoRTree.radPerDegree
              GeoRTree.distance(lat, lon, Math.sin(radLat), Math.cos(radLat), t) === 0.0f +- 0.1f
            }
        }
      }
      "return a distance to the nearest edge of the RTree bounding box if point doesn't intersect and is aligned vertically" in check {
        forAll(latLonEntryGen, latGen, lonGen) {
          (t: GeoRTreeEntry[Int], lat: Float, lon: Float) =>
            propBoolean(!geoIntersects(t, lat, lon) && alignedVertically(t, lat, lon)) ==> {
              val distancesForCorners = IndexedSeq(
                greatCircleDistance(lat, lon, t.minLat, lon),
                greatCircleDistance(lat, lon, t.maxLat, lon),
                greatCircleDistance(lat, lon, t.minLat, t.minLon),
                greatCircleDistance(lat, lon, t.minLat, t.maxLon),
                greatCircleDistance(lat, lon, t.maxLat, t.minLon),
                greatCircleDistance(lat, lon, t.maxLat, t.maxLon)
              )
              val expected = distancesForCorners.min
              val radLat = lat * GeoRTree.radPerDegree
              val result = GeoRTree.distance(lat, lon, Math.sin(radLat), Math.cos(radLat), t)
              result <= expected + 0.1f
            }
        }
      }
      "return a distance to the nearest edge of the RTree bounding box if point doesn't not intersect and is aligned horizontally" in check {
        forAll(latLonEntryGen, latGen, lonGen) {
          (t: GeoRTreeEntry[Int], lat: Float, lon: Float) =>
            propBoolean(!geoIntersects(t, lat, lon) && alignedHorizontally(t, lat, lon)) ==> {
              val distancesForCorners = IndexedSeq(
                greatCircleDistance(lat, lon, lat, t.minLon),
                greatCircleDistance(lat, lon, lat, t.maxLon),
                greatCircleDistance(lat, lon, t.minLat, t.minLon),
                greatCircleDistance(lat, lon, t.minLat, t.maxLon),
                greatCircleDistance(lat, lon, t.maxLat, t.minLon),
                greatCircleDistance(lat, lon, t.maxLat, t.maxLon)
              )
              val expected = distancesForCorners.min
              val radLat = lat * GeoRTree.radPerDegree
              val result = GeoRTree.distance(lat, lon, Math.sin(radLat), Math.cos(radLat), t)
              result <= expected + 0.1f
            }
        }
      }
      "return a distance to the nearest corner of the RTree bounding box if point doesn't not intersect and is not aligned vertically or horizontally" in check {
        forAll(latLonEntryGen, latGen, lonGen) {
          (t: GeoRTreeEntry[Int], lat: Float, lon: Float) =>
            propBoolean(!geoIntersects(t, lat, lon) && !alignedHorizontally(t, lat, lon) && !alignedVertically(t, lat, lon)) ==> {
              val distancesForCorners = IndexedSeq(
                greatCircleDistance(lat, lon, t.minLat, t.minLon),
                greatCircleDistance(lat, lon, t.minLat, t.maxLon),
                greatCircleDistance(lat, lon, t.maxLat, t.minLon),
                greatCircleDistance(lat, lon, t.maxLat, t.maxLon)
              )
              val expected = distancesForCorners.min
              val radLat = lat * GeoRTree.radPerDegree
              val result = GeoRTree.distance(lat, lon, Math.sin(radLat), Math.cos(radLat), t)
              result <= expected + 0.1f
            }
        }
      }
    }
    "update" should {
      "withdraw matched entries from a rtree" in check {
        forAll(latLonEntryListGen, latLonEntryListGen) {
          (entries1: Seq[GeoRTreeEntry[Int]], entries2: Seq[GeoRTreeEntry[Int]]) =>
            val entries12 = entries1 ++ entries2
            val expected = entries1.sorted
            GeoRTree.update(GeoRTree(entries12), entries2, Nil).entries.sorted === expected
        }
      }
      "build new rtree with old and inserted entries" in check {
        forAll(latLonEntryListGen, latLonEntryListGen) {
          (entries1: Seq[GeoRTreeEntry[Int]], entries3: Seq[GeoRTreeEntry[Int]]) =>
            val expected = (entries1 ++ entries3).sorted
            GeoRTree.update(GeoRTree(entries1), Nil, entries3).entries.sorted === expected
        }
      }
      "remove and insert at the same time properly" in check {
        forAll(latLonEntryListGen, latLonEntryListGen, latLonEntryListGen) {
          (entries1: Seq[GeoRTreeEntry[Int]], entries2: Seq[GeoRTreeEntry[Int]], entries3: Seq[GeoRTreeEntry[Int]]) =>
            val entries12 = entries1 ++ entries2
            val expected = (entries1 ++ entries3).sorted
            GeoRTree.update(GeoRTree(entries12), entries2, entries3).entries.sorted === expected
        }
      }
    }
    "asked for entries" should {
      "return all entries" in check {
        forAll(latLonEntryListGen) {
          (entries: Seq[GeoRTreeEntry[Int]]) =>
            val expected = entries.sorted
            GeoRTree(entries).entries.sorted === expected
        }
      }
    }
    "asked for nearest one" should {
      "return any of entries which intersects with point" in check {
        forAll(latLonEntryListGen, latGen, lonGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float) =>
            val sorted = entries.map(e => (distance(lat, lon, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && sorted.exists { case (d, e) => d == 0.0f }) ==> {
              val result = GeoRTree(entries).nearestOption(lat, lon).get
              sorted.map(_._2).contains(result)
            }
        }
      }
      "return the nearest entry if point does not intersect with entries" in check {
        forAll(latLonEntryListGen, latGen, lonGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float) =>
            val sorted = entries.map(e => (distance(lat, lon, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && !sorted.exists { case (d, e) => d == 0.0f }) ==> {
              GeoRTree(entries).nearestOption(lat, lon) === Some(sorted.head._2)
            }
        }
      }
      "return the nearest entry with in a specified distance limit or none if all entries are out of the limit" in check {
        forAll(latLonEntryListGen, latGen, lonGen, floatGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float, maxDist: Float) =>
            val sorted = entries.map(e => (distance(lat, lon, e), e)).filter(_._1 < maxDist).sortBy(_._1)
            propBoolean(sorted.nonEmpty) ==> {
              val result = GeoRTree(entries).nearestOption(lat, lon, maxDist)
              sorted.map { case (d, e) =>Some(e) }.contains(result)
            }
        }
      }
      "don't return any entry for empty tree" in check {
        forAll(latLonEntryListGen, latGen, lonGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float) =>
            propBoolean(entries.isEmpty) ==> {
              GeoRTree(entries).nearestOption(lat, lon) === None
            }
        }
      }
    }
    "asked for nearest K" should {
      "return up to K entries which intersects with point" in check {
        forAll(latLonEntryListGen, latGen, lonGen, positiveIntGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float, k: Int) =>
            val sorted = entries.map(e => (distance(lat, lon, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && sorted.exists { case (d, e) => d == 0.0f }) ==> {
              val result = GeoRTree(entries).nearestK(lat, lon, k)
              result.forall(sorted.map(_._2).contains)
            }
        }
      }
      "return up to K nearest entries if point does not intersect with entries" in check {
        forAll(latLonEntryListGen, latGen, lonGen, positiveIntGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float, k: Int) =>
            val sorted = entries.map(e => (distance(lat, lon, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && !sorted.exists { case (d, e) => d == 0.0f } && sorted.size == sorted.map(_._1).distinct.size) ==> {
              GeoRTree(entries).nearestK(lat, lon, k).toSet === sorted.take(k).map(_._2).toSet
            }
        }
      }
      "return up to K nearest entries with in a specified distance limit or none if all entries are out of the limit" in check {
        forAll(latLonEntryListGen, latGen, lonGen, floatGen, positiveIntGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float, maxDist: Float, k: Int) =>
            val sorted = entries.map(e => (distance(lat, lon, e), e)).filter(_._1 < maxDist).sortBy(_._1)
            propBoolean(sorted.size == sorted.map(_._1).distinct.size) ==> {
              GeoRTree(entries).nearestK(lat, lon, k, maxDist).toSet === sorted.take(k).map(_._2).toSet
            }
        }
      }
      "don't return any entry for empty tree" in check {
        forAll(latLonEntryListGen, latGen, lonGen, positiveIntGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float, k: Int) =>
            propBoolean(entries.isEmpty) ==> {
              GeoRTree(entries).nearestK(lat, lon, k) === Seq()
            }
        }
      }
    }
    "full searched by point" should {
      "receive value of all matched entries" in check {
        forAll(latLonEntryListGen, latGen, lonGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float) =>
            val expected = geoIntersects(entries, lat, lon).sorted
            propBoolean(expected.nonEmpty) ==> {
              GeoRTree(entries).searchAll(lat, lon).sorted === expected
            }
        }
      }
      "don't receive any value if no matches" in check {
        forAll(latLonEntryListGen, latGen, lonGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat: Float, lon: Float) =>
            val expected = geoIntersects(entries, lat, lon)
            propBoolean(expected.isEmpty) ==> {
              GeoRTree(entries).searchAll(lat, lon).isEmpty
            }
        }
      }
    }
    "full searched by rectangle" should {
      "receive value of all matched entries" in check {
        forAll(latLonEntryListGen, latGen, lonGen, latGen, lonGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat1: Float, lon1: Float, lat2: Float, lon2: Float) =>
            val minLat = Math.min(lat1, lat2)
            val minLon = Math.min(lon1, lon2)
            val maxLat = Math.max(lat1, lat2)
            val maxLon = Math.max(lon1, lon2)
            val expected = geoIntersects(entries, minLat, minLon, maxLat, maxLon).sorted
            propBoolean(expected.nonEmpty) ==> {
              GeoRTree(entries).searchAll(minLat, minLon, maxLat, maxLon).sorted === expected
            }
        }
      }
      "don't receive any value if no matches" in check {
        forAll(latLonEntryListGen, latGen, lonGen, latGen, lonGen) {
          (entries: Seq[GeoRTreeEntry[Int]], lat1: Float, lon1: Float, lat2: Float, lon2: Float) =>
            val minLat = Math.min(lat1, lat2)
            val minLon = Math.min(lon1, lon2)
            val maxLat = Math.max(lat1, lat2)
            val maxLon = Math.max(lon1, lon2)
            val expected = geoIntersects(entries, minLat, minLon, maxLat, maxLon)
            propBoolean(expected.isEmpty) ==> {
              GeoRTree(entries).searchAll(minLat, minLon, maxLat, maxLon).isEmpty
            }
        }
      }
    }
  }
}
