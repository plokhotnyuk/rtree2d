package com.sizmek.rtree2d.core

import com.sizmek.rtree2d.core.GeoUtils._
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class RTreeTest extends FunSuite {
  private val entries = ((1 to 100) :+ 100).map(x => RTreeEntry(x1 = x, y1 = x, x2 = x + 1.9f, y2 = x + 1.9f, value = x))
  private val rtree = RTree[Int](entries)

  test("RTreeNil.x1") {
    intercept[UnsupportedOperationException](RTree[Int](Nil).x1)
  }

  test("RTreeNil.y1") {
    intercept[UnsupportedOperationException](RTree[Int](Nil).y1)
  }

  test("RTreeNil.x2") {
    intercept[UnsupportedOperationException](RTree[Int](Nil).x2)
  }

  test("RTreeNil.y2") {
    intercept[UnsupportedOperationException](RTree[Int](Nil).y2)
  }

  test("RTreeNil.entries") {
    assert(RTree[Int](Nil).entries === Seq())
  }

  test("RTreeNil.nearest") {
    import EuclideanPlaneDistanceCalculator._
    assert(RTree[Int](Nil).nearest(0, 0) === None)
  }

  test("RTreeNil.searchAll by point") {
    assert(RTree[Int](Nil).searchAll(0, 0) === Seq())
  }

  test("RTreeNil.searchAll by rectangle") {
    assert(RTree[Int](Nil).searchAll(0, 0, 0, 0) === Seq())
  }

  test("RTreeNil.equals") {
    assert(RTree[Int](Nil) === RTree[Int](Nil))
    assert(RTree[Int](Nil) === RTree[String](Nil))
    assert(RTree[Int](Nil) !== Seq())
    assert(RTree[Int](Nil) !== RTreeEntry(1, 2, 1, 2, 5))
    assert(RTree[Int](Nil) !== RTree(Seq(RTreeEntry(1, 2, 1, 2, 5))))
  }

  test("RTreeNil.hashCode") {
    assert(RTree[Int](Nil).hashCode() === RTree[Int](Nil).hashCode())
    assert(RTree[Int](Nil).hashCode() === RTree[String](Nil).hashCode())
  }

  test("RTreeEntry.apply") {
    assert(RTreeEntry(1, 2, 1, 2, 5) === RTreeEntry(1, 2, 5))
    assert(intercept[IllegalArgumentException](RTreeEntry(Float.NaN, 2, 5)).getMessage ===
      "x should not be NaN")
    assert(intercept[IllegalArgumentException](RTreeEntry(1, Float.NaN, 5)).getMessage ===
      "y should not be NaN")
    assert(intercept[IllegalArgumentException](RTreeEntry(5, 4, 3, 2, 1)).getMessage ===
      "x2 should be greater than x1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](RTreeEntry(Float.NaN, 2, 3, 4, 5)).getMessage ===
      "x2 should be greater than x1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](RTreeEntry(1, 2, Float.NaN, 4, 5)).getMessage ===
      "x2 should be greater than x1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](RTreeEntry(Float.NaN, 2, Float.NaN, 4, 5)).getMessage ===
      "x2 should be greater than x1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](RTreeEntry(3, 4, 5, 2, 1)).getMessage ===
      "y2 should be greater than y1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](RTreeEntry(1, Float.NaN, 3, 4, 5)).getMessage ===
      "y2 should be greater than y1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](RTreeEntry(1, 2, 3, Float.NaN, 5)).getMessage ===
      "y2 should be greater than y1 and any of them should not be NaN")
    assert(intercept[IllegalArgumentException](RTreeEntry(1, Float.NaN, 3, Float.NaN, 5)).getMessage ===
      "y2 should be greater than y1 and any of them should not be NaN")
  }

  test("RTreeEntry.entries") {
    assert(RTreeEntry(1, 2, 1, 2, 5).entries === Seq(RTreeEntry(1, 2, 5)))
  }

  test("RTreeEntry.nearest") {
    import EuclideanPlaneDistanceCalculator._
    assert(RTreeEntry(1, 2, 1, 2, 5).nearest(0, 0) === Some((2.236068f, RTreeEntry(1, 2, 1, 2, 5))))
    assert(RTreeEntry(1, 2, 1, 2, 5).nearest(1, 2) === Some((0.0f, RTreeEntry(1, 2, 1, 2, 5))))
  }

  test("RTreeEntry.searchAll by point") {
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(0, 0) === Seq())
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(1, 2) === Seq(RTreeEntry(1, 2, 3, 4, 5)))
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(2, 3) === Seq(RTreeEntry(1, 2, 3, 4, 5)))
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(3, 4) === Seq(RTreeEntry(1, 2, 3, 4, 5)))
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(3, Float.NaN) === Seq())
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(Float.NaN, 3) === Seq())
  }

  test("RTreeEntry.searchAll by rectangle") {
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(-1, -1, 0, 0) === Seq())
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2) === Seq(RTreeEntry(1, 2, 3, 4, 5)))
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(2, 3, 4, 5) === Seq(RTreeEntry(1, 2, 3, 4, 5)))
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(3, 4, 5, 6) === Seq(RTreeEntry(1, 2, 3, 4, 5)))
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(Float.NaN, 4, 5, 6) === Seq())
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(3, Float.NaN, 5, 6) === Seq())
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(3, 4, Float.NaN, 6) === Seq())
    assert(RTreeEntry(1, 2, 3, 4, 5).searchAll(3, 4, 5, Float.NaN) === Seq())
  }

  test("RTreeEntry.equals") {
    assert(RTreeEntry(1, 2, 3, 4, 5) === RTreeEntry(1, 2, 3, 4, 5))
    assert(RTreeEntry(1, 2, 3, 4, 5) === RTree(Seq(RTreeEntry(1, 2, 3, 4, 5))))
    assert(RTreeEntry(1, 2, 3, 4, 5) !== RTreeEntry(1.1f, 2, 3, 4, 5))
    assert(RTreeEntry(1, 2, 3, 4, 5) !== RTreeEntry(1, 2.1f, 3, 4, 5))
    assert(RTreeEntry(1, 2, 3, 4, 5) !== RTreeEntry(1, 2, 3.1f, 4, 5))
    assert(RTreeEntry(1, 2, 3, 4, 5) !== RTreeEntry(1, 2, 3, 4.1f, 5))
    assert(RTreeEntry(1, 2, 3, 4, 5) !== RTreeEntry(1, 2, 3, 4, 50))
    assert(RTreeEntry(1, 2, 3, 4, 5) !== RTreeEntry(1, 2, 3, 4, "5"))
  }

  test("RTreeEntry.hashCode") {
    assert(RTreeEntry(1, 2, 3, 4, 5).hashCode() === RTreeEntry(1, 2, 3, 4, 5).hashCode())
    assert(RTreeEntry(1, 2, 3, 4, 5).hashCode() === RTree(Seq(RTreeEntry(1, 2, 3, 4, 5))).hashCode())
    assert(RTreeEntry(1, 2, 3, 4, 5).hashCode() !== RTreeEntry(1.1f, 2, 3, 4, 5).hashCode())
    assert(RTreeEntry(1, 2, 3, 4, 5).hashCode() !== RTreeEntry(1, 2.1f, 3, 4, 5).hashCode())
    assert(RTreeEntry(1, 2, 3, 4, 5).hashCode() !== RTreeEntry(1, 2, 3.1f, 4, 5).hashCode())
    assert(RTreeEntry(1, 2, 3, 4, 5).hashCode() !== RTreeEntry(1, 2, 3, 4.1f, 5).hashCode())
    assert(RTreeEntry(1, 2, 3, 4, 5).hashCode() !== RTreeEntry(1, 2, 3, 4, 50).hashCode())
    assert(RTreeEntry(1, 2, 3, 4, 5).hashCode() !== RTreeEntry(1, 2, 3, 4, "5").hashCode())
  }

  test("RTree.apply") {
    assert(RTree(entries).entries === entries)
    assert(RTree(entries, 4).entries === entries)
    assert(RTree(entries, 16).entries === entries)
    assert(intercept[IllegalArgumentException](RTree(entries, 1)).getMessage ===
      "nodeCapacity should be greater than 1")
  }

  test("RTree.entries") {
    assert(rtree.entries === entries)
  }

  test("RTree.nearest") {
    import EuclideanPlaneDistanceCalculator._
    assert(rtree.nearest(0, 0) === Some((1.4142135f, entries.head)))
    assert(rtree.nearest(100, 100) === Some((0.0f, entries.init.init.last)))
  }

  test("RTree.update") {
    val (entries1, entries2) = entries.splitAt(entries.size / 2)
    assert(RTree.update(RTree(entries1), Nil, entries2).entries.size === rtree.entries.size)
    assert(RTree.update(rtree, Nil, rtree.entries).entries.size === (rtree.entries ++ rtree.entries).size)
    assert(RTree.update(rtree, entries1, entries1).entries.size === entries.size)
    assert(RTree.update(rtree, entries1, Nil).entries.size === entries2.size)
    assert(RTree.update(RTree.update(rtree, Nil, rtree.entries), entries1, Nil).entries.size === (rtree.entries ++ entries2).size)
  }

  test("RTree.searchAll by point") {
    assert(rtree.searchAll(50, 50).map(_.value) === Seq(49, 50))
  }

  test("RTree.search by point") {
    var found = Seq.empty[Int]
    rtree.search(50, 50) { e =>
      found = found :+ e.value
      true
    }
    assert(found === Seq(49))
    found = Seq.empty[Int]
    rtree.search(50, 50) { e =>
      found = found :+ e.value
      false
    }
    assert(found === Seq(49, 50))
  }

  test("RTree.searchAll by rectangle") {
    assert(rtree.searchAll(50, 50, 51, 51).map(_.value) === Seq(49, 50, 51))
  }

  test("RTree.search by rectangle") {
    var found = Seq.empty[Int]
    rtree.search(50, 50, 51, 51) { e =>
      found = found :+ e.value
      true
    }
    assert(found === Seq(49))
    found = Seq.empty[Int]
    rtree.search(50, 50, 51, 51) { e =>
      found = found :+ e.value
      false
    }
    assert(found === Seq(49, 50, 51))
  }

  test("RTree.toString") {
    assert(RTree[Int](Nil).toString ===
      """RTreeNil()
        |""".stripMargin)
    assert(RTree[Int](entries.take(1)).toString ===
      """RTreeEntry(1.0,1.0,2.9,2.9,1)
        |""".stripMargin)
    assert(RTree[Int](entries.take(2)).toString ===
      """RTreeNode(1.0,1.0,3.9,3.9)
        |  RTreeEntry(1.0,1.0,2.9,2.9,1)
        |  RTreeEntry(2.0,2.0,3.9,3.9,2)
        |""".stripMargin)
  }

  test("RTree.equals") {
    intercept[UnsupportedOperationException](RTree(entries, 2) == RTree(entries, 4))
  }

  test("RTree.hashCode") {
    intercept[UnsupportedOperationException](RTree(entries).hashCode())
  }

  test("EuclideanPlaneDistanceCalculator.calculator.distance") {
    import EuclideanPlaneDistanceCalculator.calculator._
    assert(distance(0, 0, RTreeEntry(0, 0, 3)) === 0.0f)
    assert(distance(0, 0, RTreeEntry(-1, -1, 1, 1, 3)) === 0.0f)
    assert(distance(0, 0, RTreeEntry(3, 4, 5, 6, 3)) === 5f)
    intercept[UnsupportedOperationException](distance(0, 0, RTree[Int](Nil)))
  }

  test("GeodesicShpereDistanceCalculator.calculator") {
    import SphericalEarthDistanceCalculator.calculator._
    assert(distance(0, 0, RTreeEntry(0, 0, 3)) === 0.0f)
    assert(distance(0, 0, RTreeEntry(-10, -10, 10, 10, 3)) === 0.0f)
    assert(distance(0, 0, RTreeEntry(10, 10, 20, 20, 3)) === distance(0, 0, RTreeEntry(10, 10, 3)))
    assert(distance(0, 0, RTreeEntry(-20, -20, -10, -10, 3)) === distance(0, 0, RTreeEntry(-10, -10, 3)))
    assert(distance(0, 0, RTreeEntry(10, -20, 20, -10, 3)) === distance(0, 0, RTreeEntry(10, -10, 3)))
    assert(distance(0, 0, RTreeEntry(-20, 10, -10, 20, 3)) === distance(0, 0, RTreeEntry(-10, 10, 3)))
    assert(distance(0, 0, RTreeEntry(10, -10, 20, 10, 3)) === distance(0, 0, RTreeEntry(10, 0, 3)))
    assert(distance(0, 0, RTreeEntry(-20, -10, -10, 10, 3)) === distance(0, 0, RTreeEntry(-10, 0, 3)))
    assert(distance(0, 0, RTreeEntry(-10, 10, 10, 20, 3)) === distance(0, 0, RTreeEntry(0, 10, 3)))
    assert(distance(0, 0, RTreeEntry(-10, -20, 10, -10, 3)) === distance(0, 0, RTreeEntry(0, -10, 3)))
    assert(distance(0, 0, RTreeEntry(0, 180, 3)) === distance(-90, 0, RTreeEntry(90, 0, 3)))
    assert(distance(0, 0, RTreeEntry(0, 0, 3)) === distance(0, -180, RTreeEntry(0, 180, 3)) +- 0.5f)
    assert(distance(0, 0, RTreeEntry(0, 10, 3)) === distance(0, -180, RTreeEntry(-10, -160, 10, 170, 3)))
    assert(distance(0, 0, RTreeEntry(0, 10, 3)) === distance(0, 180, RTreeEntry(-10, -170, 10, 160, 3)))
    assert(distance(10, 0, RTreeEntry(10, 10, 3)) === distance(10, -180, RTreeEntry(-10, -160, 10, 170, 3)))
    assert(distance(-10, 0, RTreeEntry(-10, 10, 3)) === distance(-10, 180, RTreeEntry(-10, -170, 10, 160, 3)))
    assert(distance(50.4500f, 30.5233f, RTreeEntry(50.0614f, 19.9383f, 3)) === 753.0f +- 0.5f) // Kraków <-> Kyiv, in km
    assert(distance(34.6937f, 135.5022f, RTreeEntry(34.0522f,-118.2437f, 3)) === 9189.5f +- 0.5f) // Osaka <-> Los Angeles, in km
    intercept[UnsupportedOperationException](distance(0, 0, RTree[Int](Nil)))
  }

  test("Check precision of formulas") {
    assert(greatCircleDistance2(0, 179.99999f, 0, 180f) === greatCircleDistance1(0, 179.99999f, 0, 180f) +- 0.000005f)
    assert(greatCircleDistance2(0, 0, 0.00001f, 0.00001f) === greatCircleDistance1(0, 0, 0.00001f, 0.00001f) +- 0.000005f)
    assert(greatCircleDistance2(50.4500f, 30.5233f, 50.0614f, 19.9383f) === greatCircleDistance1(50.4500f, 30.5233f, 50.0614f, 19.9383f) +- 0.000005f)
    assert(greatCircleDistance2(34.6937f, 135.5022f, 34.0522f,-118.2437f) === greatCircleDistance1(34.6937f, 135.5022f, 34.0522f,-118.2437f) +- 0.000005f)
  }
}