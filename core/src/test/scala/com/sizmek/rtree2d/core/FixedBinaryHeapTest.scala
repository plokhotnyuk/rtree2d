package com.sizmek.rtree2d.core

import org.scalatest.FunSuite

class FixedBinaryHeapTest extends FunSuite {
  import EuclideanPlane._

  test("FixedBinaryHeap.put") {
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

  test("FixedBinaryHeap.toIndexedSeq") {
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