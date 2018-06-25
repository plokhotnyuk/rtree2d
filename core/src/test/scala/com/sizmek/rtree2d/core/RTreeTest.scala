package com.sizmek.rtree2d.core

import org.scalatest.FunSuite

class RTreeTest extends FunSuite {
  import EuclideanPlane._

  private val entries = ((1 to 100) :+ 100).map(x => entry(minX = x, minY = x, maxX = x + 1.9f, maxY = x + 1.9f, value = x))
  private val rtree = RTree[Int](entries)

  test("RTreeNil.x1") {
    intercept[UnsupportedOperationException](RTree[Int](Nil).minX)
  }

  test("RTreeNil.y1") {
    intercept[UnsupportedOperationException](RTree[Int](Nil).minY)
  }

  test("RTreeNil.x2") {
    intercept[UnsupportedOperationException](RTree[Int](Nil).maxX)
  }

  test("RTreeNil.y2") {
    intercept[UnsupportedOperationException](RTree[Int](Nil).maxY)
  }

  test("RTreeNil.entries") {
    assert(RTree[Int](Nil).entries === Seq())
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).entries(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).entries(0)).getMessage === "0")
  }

  test("RTreeNil.nearestOne") {
    assert(RTree[Int](Nil).nearestOption(0, 0) === None)
  }

  test("RTreeNil.nearestK") {
    assert(RTree[Int](Nil).nearestK(0, 0, k = 3) === Seq())
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).nearestK(0, 0, k = 3).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).nearestK(0, 0, k = 3).apply(0)).getMessage === "0")
  }

  test("RTreeNil.searchAll by point") {
    assert(RTree[Int](Nil).searchAll(0, 0) === Seq())
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).searchAll(0, 0).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).searchAll(0, 0).apply(0)).getMessage === "0")
  }

  test("RTreeNil.searchAll by rectangle") {
    assert(RTree[Int](Nil).searchAll(0, 0, 0, 0) === Seq())
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).searchAll(0, 0, 0, 0).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).searchAll(0, 0, 0, 0).apply(0)).getMessage === "0")
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
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).entries(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).entries(1)).getMessage === "1")
  }

  test("RTreeEntry.nearestOne") {
    assert(entry(1, 2, 1, 2, 5).nearestOption(0, 0, maxDist = 1f) === None)
    assert(entry(1, 2, 1, 2, 5).nearestOption(0, 0) === Some(entry(1, 2, 1, 2, 5)))
    assert(entry(1, 2, 1, 2, 5).nearestOption(1, 2) === Some(entry(1, 2, 1, 2, 5)))
  }

  test("RTreeEntry.nearestK") {
    assert(entry(1, 2, 1, 2, 5).nearestK(0, 0, k = 3, maxDist = 1f) === Seq())
    assert(entry(1, 2, 1, 2, 5).nearestK(0, 0, k = 0) === Seq())
    assert(entry(1, 2, 1, 2, 5).nearestK(0, 0, k = 3) === Seq(entry(1, 2, 1, 2, 5)))
    assert(entry(1, 2, 1, 2, 5).nearestK(1, 2, k = 3) === Seq(entry(1, 2, 1, 2, 5)))
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).nearestK(1, 2, k = 3).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).nearestK(1, 2, k = 3).apply(3)).getMessage === "3")
  }

  test("RTreeEntry.searchAll by point") {
    assert(entry(1, 2, 3, 4, 5).searchAll(0, 0) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(1, 2) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(2, 3) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, Float.NaN) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(Float.NaN, 3) === Seq())
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(1, 2).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(1, 2).apply(1)).getMessage === "1")
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
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2).apply(1)).getMessage === "1")
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
    assert(intercept[IndexOutOfBoundsException](rtree.entries(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.entries(entries.length)).getMessage === "101")
  }

  test("RTree.nearestOne") {
    assert(rtree.nearestOption(0, 0) === Some(entries.head))
    assert(rtree.nearestOption(100, 100) === Some(entries.init.init.last))
  }

  test("RTree.nearestK") {
    assert(rtree.nearestK(0, 0, k = 1) === Seq(entries(0)))
    assert(rtree.nearestK(100, 100, k = 1) === Seq(entries.init.init.last))
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
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50).apply(2)).getMessage === "2")
  }

  test("RTree.search by point") {
    var found = Seq.empty[Int]
    rtree.search(50, 50) { e =>
      found = found :+ e.value
    }
    assert(found === Seq(49, 50))
  }

  test("RTree.searchAll by rectangle") {
    assert(rtree.searchAll(50, 50, 51, 51).map(_.value) === Seq(49, 50, 51))
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50, 51, 51).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50, 51, 51).apply(3)).getMessage === "3")
  }

  test("RTree.search by rectangle") {
    var found = Seq.empty[Int]
    rtree.search(50, 50, 51, 51) { e =>
      found = found :+ e.value
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

  test("RTreeEntryBinaryHeap.put") {
    var heap = new FixedBinaryHeap[RTreeEntry[Int]](Float.PositiveInfinity, 1)
    assert(heap.put(2, entry(0, 0, 2)) == 2)
    assert(heap.put(1, entry(0, 0, 1)) == 1)
    heap = new FixedBinaryHeap[RTreeEntry[Int]](Float.PositiveInfinity, 2)
    assert(heap.put(2, entry(0, 0, 2)) == Float.PositiveInfinity)
    assert(heap.put(1, entry(0, 0, 1)) == 2)
    heap = new FixedBinaryHeap[RTreeEntry[Int]](Float.PositiveInfinity, 2)
    assert(heap.put(3, entry(0, 0, 3)) == Float.PositiveInfinity)
    assert(heap.put(2, entry(0, 0, 2)) == 3)
    assert(heap.put(1, entry(0, 0, 1)) == 2)
  }

  test("RTreeEntryBinaryHeap.toIndexedSeq") {
    val heap = new FixedBinaryHeap[RTreeEntry[Int]](Float.PositiveInfinity, 7)
    heap.put(1, entry(1, 1, 1))
    heap.put(8, entry(8, 8, 8))
    heap.put(2, entry(2, 2, 2))
    heap.put(5, entry(5, 5, 5))
    heap.put(9, entry(9, 9, 9))
    heap.put(6, entry(6, 6, 6))
    heap.put(3, entry(3, 3, 3))
    heap.put(4, entry(4, 4, 4))
    heap.put(0, entry(0, 0, 0))
    heap.put(7, entry(7, 7, 7))
    assert(heap.toIndexedSeq === Seq(
      entry(6, 6, 6),
      entry(5, 5, 5),
      entry(3, 3, 3),
      entry(1, 1, 1),
      entry(4, 4, 4),
      entry(2, 2, 2),
      entry(0, 0, 0)
    ))
  }
}