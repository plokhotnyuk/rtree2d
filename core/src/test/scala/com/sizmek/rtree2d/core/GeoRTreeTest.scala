package com.sizmek.rtree2d.core

import com.sizmek.rtree2d.core.GeoRTree.{entry, entries}
import com.sizmek.rtree2d.core.TestUtils.distance
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class GeoRTreeTest extends FunSuite {
  private val es = ((1 to 50) :+ 50).map(x => entry(x, x, x + 1.9f, x + 1.9f, value = x))
  private val rtree = GeoRTree[Int](es)

  test("GeoRTree.entry") {
    assert(entry(0, 0, 3) === entry(0, 0, 0, 0, 3))
    assert(entry(-7, -7, 7, 7, 3) === entry(-7, -7, 7, 7, 3))
    assert(entries(0, 0, distance = 778.36551f/*km*/, 3) === Seq(new GeoRTreeEntry(-7, -7, 7, 7, 3)))
    assert(entries(0, 0, distance = 7777777f/*km*/, 3) === Seq(new GeoRTreeEntry(-90, -180, 90, 180, 3)))
    assert(entries(90, 0, distance = 777f/*km*/, 3) === Seq(new GeoRTreeEntry(83.01228f, -180.0f, 90.0f, 180.0f, 3)))
    assert(entries(0, 179, distance = 777f/*km*/, 3) === Seq(new GeoRTreeEntry(-6.987719f, -180.0f, 6.987719f, -174.01228f, 3),
      new GeoRTreeEntry(-6.987719f, 172.01228f, 6.987719f, 180.0f, 3)))
    assert(entries(0, -179, distance = 777f/*km*/, 3) === Seq(new GeoRTreeEntry(-6.987719f, -180.0f, 6.987719f, -172.01228f, 3),
      new GeoRTreeEntry(-6.987719f, 174.01228f, 6.987719f, 180.0f, 3)))
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

  test("GeoRTree.distance") {
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
    assert(distance(10, 0, entry(10, 10, 3)) === distance(10, -180, entry(-10, -160, 10, 170, 3)) +- 0.5f)
    assert(distance(-10, 0, entry(-10, 10, 3)) === distance(-10, 180, entry(-10, -170, 10, 160, 3)) +- 0.5f)
    assert(distance(50.4500f, 30.5233f, entry(50.0614f, 19.9383f, 3)) === 753.0f +- 0.5f) // Kraków <-> Kyiv, in km
    assert(distance(34.6937f, 135.5022f, entry(34.0522f, -118.2437f, 3)) === 9189.5f +- 0.5f) // Osaka <-> Los Angeles, in km
    assert(intercept[UnsupportedOperationException](distance(0, 0, GeoRTree[Int](Nil)))
      .getMessage === "GeoRTreeNil.minLon")
  }

  test("GeoRTreeNil.minLat") {
    assert(intercept[UnsupportedOperationException](GeoRTree[Int](Nil).minLat)
      .getMessage === "GeoRTreeNil.minLat")
  }

  test("GeoRTreeNil.minLon") {
    assert(intercept[UnsupportedOperationException](GeoRTree[Int](Nil).minLon)
      .getMessage === "GeoRTreeNil.minLon")
  }

  test("GeoRTreeNil.maxLat") {
    assert(intercept[UnsupportedOperationException](GeoRTree[Int](Nil).maxLat)
      .getMessage === "GeoRTreeNil.maxLat")
  }

  test("GeoRTreeNil.maxLon") {
    assert(intercept[UnsupportedOperationException](GeoRTree[Int](Nil).maxLon)
      .getMessage === "GeoRTreeNil.maxLon")
  }

  test("GeoRTreeNil.entries") {
    assert(GeoRTree[Int](Nil).entries === Seq())
    assert(intercept[IndexOutOfBoundsException](GeoRTree[Int](Nil).entries(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](GeoRTree[Int](Nil).entries(0)).getMessage === "0")
  }

  test("GeoRTreeNil.nearestOne") {
    assert(GeoRTree[Int](Nil).nearestOption(0, 0) === None)
  }

  test("GeoRTreeNil.nearestK") {
    assert(GeoRTree[Int](Nil).nearestK(0, 0, k = 3) === Seq())
    assert(intercept[IndexOutOfBoundsException](GeoRTree[Int](Nil).nearestK(0, 0, k = 3).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](GeoRTree[Int](Nil).nearestK(0, 0, k = 3).apply(0)).getMessage === "0")
  }

  test("GeoRTreeNil.searchAll by point") {
    assert(GeoRTree[Int](Nil).searchAll(0, 0) === Seq())
    assert(intercept[IndexOutOfBoundsException](GeoRTree[Int](Nil).searchAll(0, 0).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](GeoRTree[Int](Nil).searchAll(0, 0).apply(0)).getMessage === "0")
  }

  test("GeoRTreeNil.searchAll by rectangle") {
    assert(GeoRTree[Int](Nil).searchAll(0, 0, 0, 0) === Seq())
    assert(intercept[IndexOutOfBoundsException](GeoRTree[Int](Nil).searchAll(0, 0, 0, 0).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](GeoRTree[Int](Nil).searchAll(0, 0, 0, 0).apply(0)).getMessage === "0")
  }

  test("GeoRTreeNil.equals") {
    assert(GeoRTree[Int](Nil) === GeoRTree[Int](Nil))
    assert(GeoRTree[Int](Nil) === GeoRTree[String](Nil))
    assert(GeoRTree[Int](Nil) !== Seq())
    assert(GeoRTree[Int](Nil) !== entry(1, 2, 1, 2, 5))
    assert(GeoRTree[Int](Nil) !== GeoRTree(Seq(entry(1, 2, 1, 2, 5))))
  }

  test("GeoRTreeNil.hashCode") {
    assert(GeoRTree[Int](Nil).hashCode() === GeoRTree[Int](Nil).hashCode())
    assert(GeoRTree[Int](Nil).hashCode() === GeoRTree[String](Nil).hashCode())
  }

  test("GeoRTreeEntry.entries") {
    assert(entry(1, 2, 1, 2, 5).entries === Seq(entry(1, 2, 5)))
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).entries(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).entries(1)).getMessage === "1")
  }

  test("GeoRTreeEntry.nearestOne") {
    assert(entry(1, 2, 1, 2, 5).nearestOption(0, 0, maxDist = 1f) === None)
    assert(entry(1, 2, 1, 2, 5).nearestOption(0, 0) === Some(entry(1, 2, 1, 2, 5)))
    assert(entry(1, 2, 1, 2, 5).nearestOption(1, 2) === Some(entry(1, 2, 1, 2, 5)))
  }

  test("GeoRTreeEntry.nearestK") {
    assert(entry(1, 2, 1, 2, 5).nearestK(0, 0, k = 3, maxDist = 1f) === Seq())
    assert(entry(1, 2, 1, 2, 5).nearestK(0, 0, k = 0) === Seq())
    assert(entry(1, 2, 1, 2, 5).nearestK(0, 0, k = 3) === Seq(entry(1, 2, 1, 2, 5)))
    assert(entry(1, 2, 1, 2, 5).nearestK(1, 2, k = 3) === Seq(entry(1, 2, 1, 2, 5)))
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).nearestK(1, 2, k = 3).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).nearestK(1, 2, k = 3).apply(3)).getMessage === "3")
  }

  test("GeoRTreeEntry.searchAll by point") {
    assert(entry(1, 2, 3, 4, 5).searchAll(0, 0) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(1, 2) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(2, 3) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, Float.NaN) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(Float.NaN, 3) === Seq())
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(1, 2).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(1, 2).apply(1)).getMessage === "1")
  }

  test("GeoRTreeEntry.searchAll by rectangle") {
    assert(entry(1, 2, 3, 4, 5).searchAll(-1, -1, 0, 0) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(2, 3, 4, 5) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4, 5, 6) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(Float.NaN, 4, 5, 6) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(3, Float.NaN, 5, 6) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4, Float.NaN, 6) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4, 5, Float.NaN) === Seq())
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2).apply(1)).getMessage === "1")
  }

  test("GeoRTreeEntry.equals") {
    assert(entry(1, 2, 3, 4, 5) === entry(1, 2, 3, 4, 5))
    assert(entry(1, 2, 3, 4, 5) === GeoRTree(Seq(entry(1, 2, 3, 4, 5))))
    assert(entry(1, 2, 3, 4, 5) !== entry(1.1f, 2, 3, 4, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2.1f, 3, 4, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3.1f, 4, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3, 4.1f, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3, 4, 50))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3, 4, "5"))
  }

  test("GeoRTreeEntry.hashCode") {
    assert(entry(1, 2, 3, 4, 5).hashCode() === entry(1, 2, 3, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() === GeoRTree(Seq(entry(1, 2, 3, 4, 5))).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1.1f, 2, 3, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2.1f, 3, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3.1f, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3, 4.1f, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3, 4, 50).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3, 4, "5").hashCode())
  }

  test("GeoRTree.apply") {
    assert(GeoRTree(es).entries === es)
    assert(GeoRTree(es, 4).entries === es)
    assert(GeoRTree(es, 8).entries === es)
    assert(intercept[IllegalArgumentException](GeoRTree(es, 1)).getMessage ===
      "nodeCapacity should be greater than 1")
  }

  test("GeoRTree.entries") {
    assert(rtree.entries === es)
    assert(intercept[IndexOutOfBoundsException](rtree.entries(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.entries(es.length)).getMessage === "51")
  }

  test("GeoRTree.nearestOne") {
    assert(rtree.nearestOption(0, 0) === Some(es.head))
    assert(rtree.nearestOption(100, 100) === Some(es.last))
  }

  test("GeoRTree.nearestK") {
    assert(rtree.nearestK(0, 0, k = 1) === Seq(es(0)))
    assert(rtree.nearestK(100, 100, k = 1) === Seq(es.last))
    assert(rtree.nearestK(0, 0, k = 0) === Seq())
    assert(rtree.nearestK(0, 0, k = 3) === Seq(
      entry(3.0f, 3.0f, 4.9f, 4.9f, 3),
      entry(1.0f, 1.0f, 2.9f, 2.9f, 1),
      entry(2.0f, 2.0f, 3.9f, 3.9f, 2)
    ))
    assert(rtree.nearestK(0, 0, k = 7) === Seq(
      entry(7.0f, 7.0f, 8.9f, 8.9f, 7),
      entry(4.0f, 4.0f, 5.9f, 5.9f, 4),
      entry(6.0f, 6.0f, 7.9f, 7.9f, 6),
      entry(1.0f, 1.0f, 2.9f, 2.9f, 1),
      entry(3.0f, 3.0f, 4.9f, 4.9f, 3),
      entry(2.0f, 2.0f, 3.9f, 3.9f, 2),
      entry(5.0f, 5.0f, 6.9f, 6.9f, 5)
    ))
    assert(intercept[IndexOutOfBoundsException](rtree.nearestK(0, 0, k = 7).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.nearestK(0, 0, k = 7).apply(7)).getMessage === "7")
  }

  test("GeoRTree.update") {
    val (entries1, entries2) = es.splitAt(es.size / 2)
    assert(GeoRTree.update(GeoRTree(entries1), Nil, entries2).entries.size === rtree.entries.size)
    assert(GeoRTree.update(rtree, Nil, rtree.entries).entries.size === (rtree.entries ++ rtree.entries).size)
    assert(GeoRTree.update(rtree, entries1, entries1).entries.size === es.size)
    assert(GeoRTree.update(rtree, entries1, Nil).entries.size === entries2.size)
    assert(GeoRTree.update(GeoRTree.update(rtree, Nil, rtree.entries), entries1, Nil).entries.size === (rtree.entries ++ entries2).size)
  }

  test("GeoRTree.searchAll by point") {
    assert(rtree.searchAll(50, 50).map(_.value) === Seq(49, 50, 50))
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50).apply(3)).getMessage === "3")
  }

  test("GeoRTree.search by point") {
    var found = Seq.empty[Int]
    rtree.search(50, 50) { e =>
      found = found :+ e.value
    }
    assert(found === Seq(49, 50, 50))
  }

  test("GeoRTree.searchAll by rectangle") {
    assert(rtree.searchAll(50, 50, 51, 51).map(_.value) === Seq(49, 50, 50))
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50, 51, 51).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50, 51, 51).apply(3)).getMessage === "3")
  }

  test("GeoRTree.search by rectangle") {
    var found = Seq.empty[Int]
    rtree.search(50, 50, 51, 51) { e =>
      found = found :+ e.value
    }
    assert(found === Seq(49, 50, 50))
  }

  test("GeoRTree.toString") {
    assert(GeoRTree[Int](Nil).toString ===
      """GeoRTreeNil()
        |""".stripMargin)
    assert(GeoRTree[Int](es.take(1)).toString ===
      """GeoRTreeEntry(1.0,1.0,2.9,2.9,1)
        |""".stripMargin)
    assert(GeoRTree[Int](es.take(2)).toString ===
      """GeoRTreeNode(1.0,1.0,3.9,3.9)
        |  GeoRTreeEntry(1.0,1.0,2.9,2.9,1)
        |  GeoRTreeEntry(2.0,2.0,3.9,3.9,2)
        |""".stripMargin)
  }

  test("GeoRTree.equals") {
    assert(intercept[UnsupportedOperationException](GeoRTree(es, 2) == GeoRTree(es, 4))
      .getMessage === "GeoRTreeNode.equals")
  }

  test("GeoRTree.hashCode") {
    assert(intercept[UnsupportedOperationException](GeoRTree(es).hashCode())
      .getMessage === "GeoRTreeNode.hashCode")
  }
}