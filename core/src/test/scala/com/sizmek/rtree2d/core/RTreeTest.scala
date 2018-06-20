package com.sizmek.rtree2d.core

import org.scalatest.FunSuite

class RTreeTest extends FunSuite {
  import EuclideanPlane._

  private val entries = ((1 to 100) :+ 100).map(x => entry(x1 = x, y1 = x, x2 = x + 1.9f, y2 = x + 1.9f, value = x))
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
    assert(RTree[Int](Nil) !== entry(1, 2, 1, 2, 5))
    assert(RTree[Int](Nil) !== RTree(Seq(entry(1, 2, 1, 2, 5))))
  }

  test("RTreeNil.hashCode") {
    assert(RTree[Int](Nil).hashCode() === RTree[Int](Nil).hashCode())
    assert(RTree[Int](Nil).hashCode() === RTree[String](Nil).hashCode())
  }

  test("RTreeEntry.entries") {
    assert(entry(1, 2, 1, 2, 5).entries === Seq(entry(1, 2, 5)))
  }

  test("RTreeEntry.nearest") {
    assert(entry(1, 2, 1, 2, 5).nearest(0, 0, maxDist = 1f) === None)
    assert(entry(1, 2, 1, 2, 5).nearest(0, 0) === Some((2.236068f, entry(1, 2, 1, 2, 5))))
    assert(entry(1, 2, 1, 2, 5).nearest(1, 2) === Some((0.0f, entry(1, 2, 1, 2, 5))))
  }

  test("RTreeEntry.searchAll by point") {
    assert(entry(1, 2, 3, 4, 5).searchAll(0, 0) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(1, 2) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(2, 3) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, Float.NaN) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(Float.NaN, 3) === Seq())
  }

  test("RTreeEntry.searchAll by rectangle") {
    assert(entry(1, 2, 3, 4, 5).searchAll(-1, -1, 0, 0) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(2, 3, 4, 5) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4, 5, 6) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(Float.NaN, 4, 5, 6) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(3, Float.NaN, 5, 6) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4, Float.NaN, 6) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4, 5, Float.NaN) === Seq())
  }

  test("RTreeEntry.equals") {
    assert(entry(1, 2, 3, 4, 5) === entry(1, 2, 3, 4, 5))
    assert(entry(1, 2, 3, 4, 5) === RTree(Seq(entry(1, 2, 3, 4, 5))))
    assert(entry(1, 2, 3, 4, 5) !== entry(1.1f, 2, 3, 4, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2.1f, 3, 4, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3.1f, 4, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3, 4.1f, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3, 4, 50))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3, 4, "5"))
  }

  test("RTreeEntry.hashCode") {
    assert(entry(1, 2, 3, 4, 5).hashCode() === entry(1, 2, 3, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() === RTree(Seq(entry(1, 2, 3, 4, 5))).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1.1f, 2, 3, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2.1f, 3, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3.1f, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3, 4.1f, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3, 4, 50).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3, 4, "5").hashCode())
  }

  test("RTree.apply") {
    assert(RTree(entries).entries === entries)
    assert(RTree(entries, 4).entries === entries)
    assert(RTree(entries, 8).entries === entries)
    assert(intercept[IllegalArgumentException](RTree(entries, 1)).getMessage ===
      "nodeCapacity should be greater than 1")
  }

  test("RTree.entries") {
    assert(rtree.entries === entries)
  }

  test("RTree.nearest") {
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
    assert(intercept[UnsupportedOperationException](RTree(entries, 2) == RTree(entries, 4))
      .getMessage === "RTreeNode.equals")
  }

  test("RTree.hashCode") {
    assert(intercept[UnsupportedOperationException](RTree(entries).hashCode())
      .getMessage === "RTreeNode.hashCode")
  }
}