package com.github.plokhotnyuk.rtree2d.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._

class GeometryTest extends AnyFunSuite {
  test("EuclideanPlane.entry") {
    import EuclideanPlane._
    assert(entry(0, 0, 3) === entry(0, 0, 0, 0, 3))
    assert(entry(0, 0, 7, 3) === entry(-7, -7, 7, 7, 3))
    assert(entry(-7, -7, 7, 7, 3) === entry(-7, -7, 7, 7, 3))
    assert(intercept[IllegalArgumentException](entry(0, 0, -7, 3))
      .getMessage === "distance should not be less than 0 or NaN")
    assert(intercept[IllegalArgumentException](entry(7, -7, -7, 7, 3))
      .getMessage === "maxX should be greater than minX and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](entry(-7, 7, 7, -7, 3))
      .getMessage === "maxY should be greater than minY and any of them should not be NaN")
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
      .getMessage === "maxX should be greater than minX and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](entry(-7, Float.NaN, 7, 7, 3))
      .getMessage === "maxY should be greater than minY and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, Float.NaN, 7, 3))
      .getMessage === "maxX should be greater than minX and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, 7, Float.NaN, 3))
      .getMessage === "maxY should be greater than minY and any of them should not be NaN")
  }

  test("EuclideanPlane.distanceCalculator") {
    import EuclideanPlane._
    import EuclideanPlane.distanceCalculator._
    assert(distance(0, 0, entry(0, 0, 3)) === 0.0f)
    assert(distance(0, 0, entry(-1, -1, 1, 1, 3)) === 0.0f)
    assert(distance(0, 0, entry(3, 4, 5, 6, 3)) === 5f)
    assert(intercept[UnsupportedOperationException](distance(0, 0, RTree[Int](Nil)))
      .getMessage === "RTreeNil.minX")
  }

  test("SphericalEarth.entry") {
    import SphericalEarth._
    assert(entry(0, 0, 3) === entry(0, 0, 0, 0, 3))
    assert(entry(-7, -7, 7, 7, 3) === entry(-7, -7, 7, 7, 3))
    assert(entries(0, 0, distance = 778.36551f/*km*/, 3) === Seq(entry(-7, -7, 7, 7, 3)))
    assert(entries(0, 0, distance = 7777777f/*km*/, 3) === Seq(entry(-90, -180, 90, 180, 3)))
    assert(entries(90, 0, distance = 777f/*km*/, 3) === Seq(entry(83.01228f, -180.0f, 90.0f, 180.0f, 3)))
    assert(entries(0, 179, distance = 777f/*km*/, 3) === Seq(entry(-6.987719f, -180.0f, 6.987719f, -174.01228f, 3),
      entry(-6.987719f, 172.01228f, 6.987719f, 180.0f, 3)))
    assert(entries(0, -179, distance = 777f/*km*/, 3) === Seq(entry(-6.987719f, -180.0f, 6.987719f, -172.01228f, 3),
      entry(-6.987719f, 174.01228f, 6.987719f, 180.0f, 3)))
    assert(intercept[IllegalArgumentException](entry(-666, 0, 3))
      .getMessage === "lat should not be out of range from -90 to 90 or NaN")
    assert(intercept[IllegalArgumentException](entry(666, 0, 3))
      .getMessage === "lat should not be out of range from -90 to 90 or NaN")
    assert(intercept[IllegalArgumentException](entry(0, -666, 3))
      .getMessage === "lon should not be out of range from -180 to 180 or NaN")
    assert(intercept[IllegalArgumentException](entry(0, 666, 3))
      .getMessage === "lon should not be out of range from -180 to 180 or NaN")
    assert(intercept[IllegalArgumentException](entry(7, -7, -7, 7, 3))
      .getMessage === "maxLat should not be greater than 90 or less than minLat or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, 666, 7, 3))
      .getMessage === "maxLat should not be greater than 90 or less than minLat or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, 7, 7, -7, 3))
      .getMessage === "maxLon should not be greater than 180 or less than minLat  or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, 7, 666, 3))
      .getMessage === "maxLon should not be greater than 180 or less than minLat  or NaN")
    assert(intercept[IllegalArgumentException](entries(0, 0, distance = -666/*km*/, 3))
      .getMessage === "distance should not be less than 0 or NaN")
    assert(intercept[IllegalArgumentException](entry(Float.NaN, 0, 3))
      .getMessage === "lat should not be out of range from -90 to 90 or NaN")
    assert(intercept[IllegalArgumentException](entry(0, Float.NaN, 3))
      .getMessage === "lon should not be out of range from -180 to 180 or NaN")
    assert(intercept[IllegalArgumentException](entry(Float.NaN, -7, 7, 7, 3))
      .getMessage === "minLat should not be less than -90 or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, Float.NaN, 7, 7, 3))
      .getMessage === "minLon should not be less than -180 or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, Float.NaN, 7, 3))
      .getMessage === "maxLat should not be greater than 90 or less than minLat or NaN")
    assert(intercept[IllegalArgumentException](entry(-7, -7, 7, Float.NaN, 3))
      .getMessage === "maxLon should not be greater than 180 or less than minLat  or NaN")
    assert(intercept[IllegalArgumentException](entries(Float.NaN, 0, distance = 778.36551f/*km*/, 3))
      .getMessage === "lat should not be out of range from -90 to 90 or NaN")
    assert(intercept[IllegalArgumentException](entries(0, Float.NaN, distance = 778.36551f/*km*/, 3))
      .getMessage === "lon should not be out of range from -180 to 180 or NaN")
    assert(intercept[IllegalArgumentException](entries(0, 0, Float.NaN, 3))
      .getMessage === "distance should not be less than 0 or NaN")
    assert(intercept[IndexOutOfBoundsException](entries(0, 0, distance = 778.36551f/*km*/, 3).apply(1))
      .getMessage === "1")
    assert(intercept[IndexOutOfBoundsException](entries(0, 179, distance = 777f/*km*/, 3).apply(2))
      .getMessage === "2")
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
    // Expected distances calculated by Vincenty’s formula: http://www.cqsrg.org/tools/GCDistance/
    assert(distance(50.0663f, -5.7148f, entry(58.6440f, -3.0701f, 3)) === 969.955f +- 1.1f) // Sennen, UK <-> John o' Groats, UK, in km
    assert(distance(50.0663f, -5.7148f, entry(51.5074f, -0.1278f, 3)) === 425.216f +- 1.2f) // Sennen, UK <-> London, UK, in km
    assert(distance(50.0663f, -5.7148f, entry(50.0614f, 19.9383f, 3)) === 1827.700f +- 5.7f) // Sennen, UK <-> Krakow, UK, in km
    assert(distance(50.4500f, 30.5233f, entry(50.0614f, 19.9383f, 3)) === 755.461f +- 2.4f) // Kyiv, UA <-> Kraków, PL, in km
    assert(distance(34.6937f, 135.5022f, entry(34.0522f, -118.2437f, 3)) === 9209.079f +- 20f) // Osaka, JP <-> Los Angeles, US, in km
    assert(distance(41.9028f, 12.4964f, entry(-34.6037f, -58.3816f, 3)) === 11131.599f +- 20f) // Rome, IT <-> Buenos Aires, AR, in km
    assert(distance(90, 0, entry(-90, 0, 3)) === 20003.931f +- 12f) // North Pole <-> South Pole, in km
    assert(distance(0, 0, entry(0, 180, 3)) === 20037.508f +- 23f) // Null Island <-> Anti-meridian, in km
    assert(intercept[UnsupportedOperationException](distance(0, 0, RTree[Int](Nil)))
      .getMessage === "RTreeNil.minY")
    // The issue test: https://github.com/plokhotnyuk/rtree2d/issues/262
    assert(distance(18.0f, -74.3f, entry(17.84f, -67.38f, 18.62f, -65.51f, 3)) === 731.6392f +- 1.0f)
  }

  test("EllipsoidalEarth.radius") {
    import EllipsoidalEarth._
    assert(radius(0) === 6378.1370)
    assert(radius(90) === 6356.7523)
    assert(radius(35.377755f) === 6371.0088 +- 0.000005)
  }
}