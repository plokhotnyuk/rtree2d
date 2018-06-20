package com.sizmek.rtree2d.core

import org.scalatest.FunSuite
import org.scalatest.Matchers._

class GeometryTest extends FunSuite {
  test("EuclideanPlane.entry") {
    import EuclideanPlane._
    assert(entry(0, 0, 3) === entry(0, 0, 0, 0, 3))
    assert(entry(0, 0, 7, 3) === entry(-7, -7, 7, 7, 3))
    assert(entry(-7, -7, 7, 7, 3) === entry(-7, -7, 7, 7, 3))
    assert(intercept[IllegalArgumentException](entry(0, 0, -7, 3))
      .getMessage === "distance should not be less than 0 or NaN")
    assert(intercept[IllegalArgumentException](entry(7, -7, -7, 7, 3))
      .getMessage === "x2 should be greater than x1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](entry(-7, 7, 7, -7, 3))
      .getMessage === "y2 should be greater than y1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](entry(Float.NaN, 0, 3))
      .getMessage === "x should not be NaN")
    assert(intercept[IllegalArgumentException](entry(0, Float.NaN, 3))
      .getMessage === "y should not be NaN")
    assert(intercept[IllegalArgumentException](entry(Float.NaN, 0, -7, 3))
      .getMessage === "x should not be NaN")
    assert(intercept[IllegalArgumentException](entry(0, Float.NaN, -7, 3))
      .getMessage === "y should not be NaN")
    assert(intercept[IllegalArgumentException](entry(0, 0, Float.NaN, 3))
      .getMessage === "distance should not be less than 0 or NaN")
    assert(intercept[IllegalArgumentException](entry(Float.NaN, -7, 7, 7, 3))
      .getMessage === "x2 should be greater than x1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](entry(-7, Float.NaN, 7, 7, 3))
      .getMessage === "y2 should be greater than y1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, Float.NaN, 7, 3))
      .getMessage === "x2 should be greater than x1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, 7, Float.NaN, 3))
      .getMessage === "y2 should be greater than y1 and any of them should not be NaN")
  }

  test("EuclideanPlane.distanceCalculator") {
    import EuclideanPlane._
    import EuclideanPlane.distanceCalculator._
    assert(distance(0, 0, entry(0, 0, 3)) === 0.0f)
    assert(distance(0, 0, entry(-1, -1, 1, 1, 3)) === 0.0f)
    assert(distance(0, 0, entry(3, 4, 5, 6, 3)) === 5f)
    assert(intercept[UnsupportedOperationException](distance(0, 0, RTree[Int](Nil)))
      .getMessage === "RTreeNil.y1")
  }

  test("SphericalEarth.entry") {
    import SphericalEarth._
    assert(entry(0, 0, 3) === entry(0, 0, 0, 0, 3))
    assert(entry(-7, -7, 7, 7, 3) === entry(-7, -7, 7, 7, 3))
    assert(intercept[IllegalArgumentException](entry(7, -7, -7, 7, 3))
      .getMessage === "maxLat should not be greater than -90 or less than minLat or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, 7, 7, -7, 3))
      .getMessage === "maxLon should not be greater than -180 or less than minLat  or NaN")
    assert(intercept[IllegalArgumentException](entry(Float.NaN, 0, 3))
      .getMessage === "lat should not be out of range from -90 to 90 or NaN")
    assert(intercept[IllegalArgumentException](entry(0, Float.NaN, 3))
      .getMessage === "lon should not be out of range from -180 to 180 or NaN")
    assert(intercept[IllegalArgumentException](entry(Float.NaN, -7, 7, 7, 3))
      .getMessage === "minLat should not be less than -90 or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, Float.NaN, 7, 7, 3))
      .getMessage === "minLon should not be less than -180 or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, Float.NaN, 7, 3))
      .getMessage === "maxLat should not be greater than -90 or less than minLat or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, 7, Float.NaN, 3))
      .getMessage === "maxLon should not be greater than -180 or less than minLat  or NaN")
  }

  test("SphericalEarth.distanceCalculator") {
    import SphericalEarth._
    import SphericalEarth.distanceCalculator._
    assert(distance(0, 0, entry(0, 0, 3)) === 0.0f)
    assert(distance(0, 0, entry(-10, -10, 10, 10, 3)) === 0.0f)
    assert(distance(0, 0, entry(10, 10, 20, 20, 3)) === distance(0, 0, entry(10, 10, 3)))
    assert(distance(0, 0, entry(-20, -20, -10, -10, 3)) === distance(0, 0, entry(-10, -10, 3)))
    assert(distance(0, 0, entry(10, -20, 20, -10, 3)) === distance(0, 0, entry(10, -10, 3)))
    assert(distance(0, 0, entry(-20, 10, -10, 20, 3)) === distance(0, 0, entry(-10, 10, 3)))
    assert(distance(0, 0, entry(10, -10, 20, 10, 3)) === distance(0, 0, entry(10, 0, 3)))
    assert(distance(0, 0, entry(-20, -10, -10, 10, 3)) === distance(0, 0, entry(-10, 0, 3)))
    assert(distance(0, 0, entry(-10, 10, 10, 20, 3)) === distance(0, 0, entry(0, 10, 3)))
    assert(distance(0, 0, entry(-10, -20, 10, -10, 3)) === distance(0, 0, entry(0, -10, 3)))
    assert(distance(0, 0, entry(0, 180, 3)) === distance(-90, 0, entry(90, 0, 3)))
    assert(distance(0, 0, entry(0, 0, 3)) === distance(0, -180, entry(0, 180, 3)) +- 0.5f)
    assert(distance(0, 0, entry(0, 10, 3)) === distance(0, -180, entry(-10, -160, 10, 170, 3)))
    assert(distance(0, 0, entry(0, 10, 3)) === distance(0, 180, entry(-10, -170, 10, 160, 3)))
    assert(distance(10, 0, entry(10, 10, 3)) === distance(10, -180, entry(-10, -160, 10, 170, 3)))
    assert(distance(-10, 0, entry(-10, 10, 3)) === distance(-10, 180, entry(-10, -170, 10, 160, 3)))
    assert(distance(50.4500f, 30.5233f, entry(50.0614f, 19.9383f, 3)) === 753.0f +- 0.5f) // Kraków <-> Kyiv, in km
    assert(distance(34.6937f, 135.5022f, entry(34.0522f, -118.2437f, 3)) === 9189.5f +- 0.5f) // Osaka <-> Los Angeles, in km
    assert(intercept[UnsupportedOperationException](distance(0, 0, RTree[Int](Nil)))
      .getMessage === "RTreeNil.y1")
  }
}