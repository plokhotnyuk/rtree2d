package com.github.plokhotnyuk.rtree2d.core

import TestUtils._
import org.scalacheck.Gen
import org.scalacheck.Prop._
import matchers.should.Matchers._
import org.scalatestplus.scalacheck.Checkers
import org.scalatest.matchers
import org.scalatest.wordspec.AnyWordSpec

class GeometryCheckers extends AnyWordSpec with Checkers {
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000)
  "EuclideanPlane.distanceCalculator" when {
    "asked to calculate distance from point to an RTree" should {
      "return a distance to a nearest part of the RTree bounding box or 0 if the point is inside it" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            val t = RTree(entries)
            propBoolean(entries.nonEmpty && !intersects(t, x, y)) ==> {
              val expected = euclideanDistance(x, y, t)
              EuclideanPlane.distanceCalculator.distance(x, y, t) === expected +- 0.001f
            }
        }
      }
    }
  }
  "SphericalEarth.distanceCalculator" when {
    "asked to calculate distance from point to an RTree" should {
      "return 0 if the point is inside it" in check {
        forAll(latLonEntryGen, Gen.choose[Float](0, 1), Gen.choose[Float](0, 1)) {
          (t: RTreeEntry[Int], rdx: Float, rdy: Float) =>
            val lat = t.minX + rdx * (t.maxX - t.minX)
            val lon = t.minY + rdy * (t.maxY - t.minY)
            propBoolean(intersects(t, lat, lon)) ==> {
              SphericalEarth.distanceCalculator.distance(lat, lon, t) === 0.0f +- 0.1f
            }
        }
      }
      "return a distance to the nearest edge of the RTree bounding box if point doesn't intersect and is aligned vertically" in check {
        forAll(latLonEntryGen, latGen, lonGen) {
          (t: RTreeEntry[Int], lat: Float, lon: Float) =>
            propBoolean(!intersects(t, lat, lon) && alignedVertically(t, lat, lon)) ==> {
              val distancesForCorners = IndexedSeq(
                greatCircleDistance(lat, lon, t.minX, lon),
                greatCircleDistance(lat, lon, t.maxX, lon),
                greatCircleDistance(lat, lon, t.minX, t.minY),
                greatCircleDistance(lat, lon, t.minX, t.maxY),
                greatCircleDistance(lat, lon, t.maxX, t.minY),
                greatCircleDistance(lat, lon, t.maxX, t.maxY)
              )
              val expected = distancesForCorners.min
              val result = SphericalEarth.distanceCalculator.distance(lat, lon, t)
              result <= expected + 0.1f
            }
        }
      }
      "return a distance to the nearest edge of the RTree bounding box if point doesn't not intersect and is aligned horizontally" in check {
        forAll(latLonEntryGen, latGen, lonGen) {
          (t: RTreeEntry[Int], lat: Float, lon: Float) =>
            propBoolean(!intersects(t, lat, lon) && alignedHorizontally(t, lat, lon)) ==> {
              val distancesForCorners = IndexedSeq(
                greatCircleDistance(lat, lon, lat, t.minY),
                greatCircleDistance(lat, lon, lat, t.maxY),
                greatCircleDistance(lat, lon, t.minX, t.minY),
                greatCircleDistance(lat, lon, t.minX, t.maxY),
                greatCircleDistance(lat, lon, t.maxX, t.minY),
                greatCircleDistance(lat, lon, t.maxX, t.maxY)
              )
              val expected = distancesForCorners.min
              val result = SphericalEarth.distanceCalculator.distance(lat, lon, t)
              result <= expected + 0.1f
            }
        }
      }
      "return a distance to the nearest corner of the RTree bounding box if point doesn't not intersect and is not aligned vertically or horizontally" in check {
        forAll(latLonEntryGen, latGen, lonGen) {
          (t: RTreeEntry[Int], lat: Float, lon: Float) =>
            propBoolean(!intersects(t, lat, lon) && !alignedHorizontally(t, lat, lon) && !alignedVertically(t, lat, lon)) ==> {
              val distancesForCorners = IndexedSeq(
                greatCircleDistance(lat, lon, t.minX, t.minY),
                greatCircleDistance(lat, lon, t.minX, t.maxY),
                greatCircleDistance(lat, lon, t.maxX, t.minY),
                greatCircleDistance(lat, lon, t.maxX, t.maxY)
              )
              val expected = distancesForCorners.min
              val result = SphericalEarth.distanceCalculator.distance(lat, lon, t)
              result <= expected + 0.1f
            }
        }
      }
    }
  }
}
