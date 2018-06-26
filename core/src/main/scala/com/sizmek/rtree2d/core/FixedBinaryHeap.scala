package com.sizmek.rtree2d.core

/**
  * A binary heap collection of distance/entry pairs.
  *
  * @param maxDist an initial limit of distance
  * @param maxSize a maximum size of the heap
  * @tparam A a type of th value being put in entries
  */
class FixedBinaryHeap[A <: AnyRef](maxDist: Float, maxSize: Int) {
  private[this] var size = 0
  private[this] var distances = new Array[Float](16)
  private[this] var entries = new Array[AnyRef](16)

  def put(d: Float, e: A): Float = {
    var distances = this.distances
    var entries = this.entries
    if (size < maxSize) {
      size += 1
      if (size >= distances.length) {
        val newSize = Math.min(distances.length << 1, maxSize + 1)
        distances = java.util.Arrays.copyOf(distances, newSize)
        entries = java.util.Arrays.copyOf(entries, newSize)
        this.distances = distances
        this.entries = entries
      }
      var i = size
      var j = i >> 1
      while (j > 0 && d >= distances(j)) {
        distances(i) = distances(j)
        entries(i) = entries(j)
        i = j
        j >>= 1
      }
      distances(i) = d
      entries(i) = e
      if (size == maxSize) distances(1) else maxDist
    } else if (size > 0 && d < distances(1)) {
      var i = 1
      var j = i << 1
      var k = j + 1
      if (k <= size && distances(k) >= distances(j)) j = k
      while (j <= size && distances(j) >= d) {
        distances(i) = distances(j)
        entries(i) = entries(j)
        i = j
        j = i << 1
        k = j + 1
        if (k <= size && distances(k) >= distances(j)) j = k
      }
      distances(i) = d
      entries(i) = e
      distances(1)
    } else maxDist
  }

  def toIndexedSeq: IndexedSeq[A] = new AdaptedShiftedIndexedSeq[AnyRef, A](entries, size)
}

private[core] class FixedIndexedSeq[A](array: Array[A], size0: Int) extends IndexedSeq[A] {
  override def length: Int = size0

  override def apply(idx: Int): A =
    if (idx < 0 || idx >= size0) throw new IndexOutOfBoundsException(idx.toString)
    else array(idx)
}

private[core] class AdaptedIndexedSeq[A, B <: A](array: Array[A]) extends IndexedSeq[B] {
  override def length: Int = array.length

  override def apply(idx: Int): B =
    if (idx < 0 || idx >= array.length) throw new IndexOutOfBoundsException(idx.toString)
    else array(idx).asInstanceOf[B]
}

private[core] class AdaptedShiftedIndexedSeq[A, B <: A](array: Array[A], size0: Int) extends IndexedSeq[B] {
  override def length: Int = size0

  override def apply(idx: Int): B =
    if (idx < 0 || idx >= size0) throw new IndexOutOfBoundsException(idx.toString)
    else array(idx + 1).asInstanceOf[B]
}

private[core] class DejaVuCounter {
  private[this] var n: Int = _

  def inc(): Unit = n += 1

  def decIfPositive(): Boolean =
    if (n > 0) {
      n -= 1
      false
    } else true
}
