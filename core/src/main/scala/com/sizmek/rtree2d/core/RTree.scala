package com.sizmek.rtree2d.core

import java.lang.Float.{intBitsToFloat, floatToRawIntBits}
import java.lang.Math._
import java.util
import java.util.Comparator

import scala.annotation.tailrec
import scala.collection.mutable

object RTree {
  /**
    * Construct an RTree from a sequence of entries using STR packing.
    *
    * @param entries the sequence of entries
    * @param nodeCapacity the maximum number of children nodes (16 by default)
    * @tparam A a type of values being put in the tree
    * @return an RTree instance
    */
  def apply[A](entries: Iterable[RTreeEntry[A]], nodeCapacity: Int = 16): RTree[A] = {
    if (nodeCapacity <= 1) throw new IllegalArgumentException("nodeCapacity should be greater than 1")
    pack(entries.toArray[RTree[A]], nodeCapacity, xComparator[A], yComparator[A])
  }

  /**
    * Update an RTree by withdrawing matched entries specified by the `remove` argument and adding entries from the
    * `insert` argument, than repack resulting entries to a new RTree using STR packing.
    *
    * @param rtree the RTree
    * @param remove the sequence of entries to remove
    * @param insert the sequence of entries to insert
    * @param nodeCapacity the maximum number of children nodes (16 by default)
    * @tparam A a type of values being put in the tree
    * @return an RTree instance
    */
  def update[A](rtree: RTree[A], remove: Iterable[RTreeEntry[A]] = Nil, insert: Iterable[RTreeEntry[A]] = Nil,
                nodeCapacity: Int = 16): RTree[A] =
    if ((rtree.isEmpty || remove.isEmpty) && insert.isEmpty) rtree
    else if (rtree.isEmpty && remove.isEmpty) {
      pack(insert.toArray[RTree[A]], nodeCapacity, xComparator[A], yComparator[A])
    } else if (remove.isEmpty) {
      val es1 = RTree.lastLevel(rtree)
      val l1 = es1.length
      val es = util.Arrays.copyOf(es1, l1 + insert.size)
      insert.copyToArray(es, l1)
      pack(es, nodeCapacity, xComparator[A], yComparator[A])
    } else {
      val cs = new mutable.AnyRefMap[RTree[A], DejaVuCounter](remove.size)
      remove.foreach(e => cs.getOrElseUpdate(e, new DejaVuCounter).inc())
      val es1 = RTree.lastLevel(rtree)
      val l1 = es1.length
      var n = insert.size
      val es = new Array[RTree[A]](l1 + n)
      insert.copyToArray(es, 0)
      var i = 0
      while (i < l1) {
        val e = es1(i)
        val optC = cs.get(e)
        if (optC.isEmpty || optC.get.decIfPositive()) {
          es(n) = e
          n += 1
        }
        i += 1
      }
      pack(if (es.length == n) es else util.Arrays.copyOf(es, n), nodeCapacity, xComparator[A], yComparator[A])
    }

  @tailrec
  private[core] def lastLevel[A](t: RTree[A]): Array[RTree[A]] = t match {
    case tn: RTreeNode[A] => tn.level(0) match {
      case _: RTreeEntry[A] => tn.level
      case n => lastLevel(n)
    }
    case e: RTreeEntry[A] => Array[RTree[A]](e)
    case _ => new Array[RTree[A]](0)
  }

  @tailrec
  private[core] def appendSpaces(sb: java.lang.StringBuilder, n: Int): java.lang.StringBuilder =
    if (n > 0) appendSpaces(sb.append(' '), n - 1)
    else sb

  @tailrec
  private[this] def pack[A](level: Array[RTree[A]], nodeCapacity: Int, xComp: Comparator[RTree[A]],
                            yComp: Comparator[RTree[A]]): RTree[A] = {
    val l = level.length
    if (l == 0) new RTreeNil
    else if (l <= nodeCapacity) packNode(level, 0, l)
    else {
      util.Arrays.sort(level, xComp)
      val nodeCount = ceil(l.toFloat / nodeCapacity).toInt
      val sliceCapacity = ceil(sqrt(nodeCount)).toInt * nodeCapacity
      val nextLevel = new Array[RTree[A]](nodeCount)
      var i, j = 0
      do {
        val sliceTo = min(i + sliceCapacity, l)
        util.Arrays.sort(level, i, sliceTo, yComp)
        do {
          val packTo = min(i + nodeCapacity, sliceTo)
          nextLevel(j) = packNode(level, i, packTo)
          j += 1
          i = packTo
        } while (i < sliceTo)
      } while (i < l)
      pack(nextLevel, nodeCapacity, xComp, yComp)
    }
  }

  private[this] def packNode[A](level: Array[RTree[A]], from: Int, to: Int): RTree[A] = {
    var t = level(from)
    var i = from + 1
    if (i == to) t
    else {
      var y1 = t.y1
      var y2 = t.y2
      var x1 = t.x1
      var x2 = t.x2
      do {
        t = level(i)
        if (y1 > t.y1) y1 = t.y1
        if (y2 < t.y2) y2 = t.y2
        if (x1 > t.x1) x1 = t.x1
        if (x2 < t.x2) x2 = t.x2
        i += 1
      } while (i < to)
      new RTreeNode(x1, y1, x2, y2, level, from, to)
    }
  }

  private[this] def xComparator[A]: Comparator[RTree[A]] = new Comparator[RTree[A]] {
    override def compare(t1: RTree[A], t2: RTree[A]): Int = floatToRawIntBits((t1.x1 + t1.x2) - (t2.x1 + t2.x2))
  }

  private[this] def yComparator[A]: Comparator[RTree[A]] = new Comparator[RTree[A]] {
    override def compare(t1: RTree[A], t2: RTree[A]): Int = floatToRawIntBits((t1.y1 + t1.y2) - (t2.y1 + t2.y2))
  }
}

/**
  * In-memory immutable r-tree with the minimal bounding rectangle (MBR).
  *
  * @tparam A a type of values being put in the tree
  */
sealed trait RTree[A] {
  /**
    * @return x value of the lower left point of the MBR
    */
  def x1: Float

  /**
    * @return y value of the lower left point of the MBR
    */
  def y1: Float

  /**
    * @return x value of the upper right point of the MBR
    */
  def x2: Float

  /**
    * @return y value of the upper right point of the MBR
    */
  def y2: Float

  def isEmpty: Boolean

  /**
    * Returns an option of the nearest R-tree entry for the given point.
    *
    * Search distance can be limited by the `maxDist` parameter.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param maxDist an exclusive limit of the distance (infinity by default)
    * @param distCalc a distance calculator provided according to geometry of indexed entries
    * @return an option of the nearest entry
    */
  def nearestOption(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
                   (implicit distCalc: DistanceCalculator): Option[RTreeEntry[A]] = {
    var res: RTreeEntry[A] = null
    nearest(x, y, maxDist)((d, e) => {
      res = e
      d
    })
    if (res eq null) None
    else new Some(res)
  }

  /**
    * Returns a sequence of up to the specified number of nearest R-tree entries for the given point.
    *
    * Search distance can be limited by the `maxDist` parameter.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param k a maximum number of nearest entries to collect
    * @param maxDist an exclusive limit of the distance (infinity by default)
    * @param distCalc a distance calculator provided according to geometry of indexed entries
    * @return a sequence of the nearest entries
    */
  def nearestK(x: Float, y: Float, k: Int, maxDist: Float = Float.PositiveInfinity)
              (implicit distCalc: DistanceCalculator): IndexedSeq[RTreeEntry[A]] =
    if (k <= 0) Vector.empty
    else new RTreeEntryBinaryHeap[A](maxDist, k) {
      nearest(x, y, maxDist)((d, e) => put(d, e))
    }.toIndexedSeq

  /**
    * Returns a distance to the nearest R-tree entry for the given point.
    *
    * Search distance can be limited by the `maxDist` parameter.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param maxDist an exclusive limit of the distance (infinity by default)
    * @param f the function that receives found values and their distances and returns the current maximum distance
    * @param distCalc a distance calculator provided according to geometry of indexed entries
    * @return the distance to a found entry or initially submitted value of maxDist
    */
  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, RTreeEntry[A]) => Float)(implicit distCalc: DistanceCalculator): Float

  /**
    * Call the provided `f` function with entries whose MBR contains the given point.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param f the function that receives found values
    */
  def search(x: Float, y: Float)(f: RTreeEntry[A] => Unit): Unit

  /**
    * Call the provided `f` function with entries whose MBR intersects with the given point.
    *
    * @param x1 x value of the lower left point of the given rectangle
    * @param y1 y value of the lower left point of the given rectangle
    * @param x2 x value of the upper right point of the given rectangle
    * @param y2 y value of the upper right point of the given rectangle
    * @param f the function that receives found values
    */
  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Unit): Unit

  /**
    * Returns a sequence of all entries in the R-tree whose MBR contains the given point.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @return a sequence of found values
    */
  def searchAll(x: Float, y: Float): IndexedSeq[RTreeEntry[A]] = new IndexedSeq[RTreeEntry[A]] {
    private[this] var size0: Int = _
    private[this] var array: Array[RTreeEntry[A]] = new Array[RTreeEntry[A]](16)

    RTree.this.search(x, y) { v =>
      if (size0 + 1 >= array.length) array = java.util.Arrays.copyOf(array, size0 << 1)
      array(size0) = v
      size0 += 1
    }

    override def length: Int = size0

    override def apply(idx: Int): RTreeEntry[A] =
      if (idx >= size0) throw new IndexOutOfBoundsException(idx.toString)
      else array(idx)
  }

  /**
    * Returns a sequence of all entries in the R-tree whose MBR intersects the given rectangle.
    *
    * @param x1 x value of the lower left point of the given rectangle
    * @param y1 y value of the lower left point of the given rectangle
    * @param x2 x value of the upper right point of the given rectangle
    * @param y2 y value of the upper right point of the given rectangle
    * @return a sequence of found values
    */
  def searchAll(x1: Float, y1: Float, x2: Float, y2: Float): IndexedSeq[RTreeEntry[A]] = new IndexedSeq[RTreeEntry[A]] {
    private[this] var size0: Int = _
    private[this] var array: Array[RTreeEntry[A]] = new Array[RTreeEntry[A]](16)

    RTree.this.search(x1, y1, x2, y2) { v =>
      if (size0 + 1 >= array.length) array = java.util.Arrays.copyOf(array, size0 << 1)
      array(size0) = v
      size0 += 1
    }

    override def length: Int = size0

    override def apply(idx: Int): RTreeEntry[A] =
      if (idx >= size0) throw new IndexOutOfBoundsException(idx.toString)
      else array(idx)
  }

  /**
    * @return a sequence of all entries
    */
  def entries: IndexedSeq[RTreeEntry[A]] = new IndexedSeq[RTreeEntry[A]] {
    private[this] val entries: Array[RTree[A]] = RTree.lastLevel(RTree.this)

    override def length: Int = entries.length

    override def apply(idx: Int): RTreeEntry[A] = entries(idx).asInstanceOf[RTreeEntry[A]]
  }

  /**
    * Appends a prettified string representation of the r-tree.
    *
    * @param sb a string builder to append to
    * @param indent a size of indent step for each level or 0 if indenting is not required
    * @return the provided string builder
    */
  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder

  /**
    * @return a prettified string representation of the r-tree
    */
  override def toString: String = pretty(new java.lang.StringBuilder, 0).toString
}

private final case class RTreeNil[A]() extends RTree[A] {
  def x1: Float = throw new UnsupportedOperationException("RTreeNil.x1")

  def y1: Float = throw new UnsupportedOperationException("RTreeNil.y1")

  def x2: Float = throw new UnsupportedOperationException("RTreeNil.x2")

  def y2: Float = throw new UnsupportedOperationException("RTreeNil.y2")

  def isEmpty: Boolean = true

  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, RTreeEntry[A]) => Float)(implicit distCalc: DistanceCalculator): Float = maxDist

  def search(x: Float, y: Float)(f: RTreeEntry[A] => Unit): Unit = ()

  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Unit): Unit = ()

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder =
    RTree.appendSpaces(sb, indent).append("RTreeNil()\n")
}

/**
  * Create an entry for a rectangle and a value.
  *
  * @param x1 x value of the lower left point of the given rectangle
  * @param y1 y value of the lower left point of the given rectangle
  * @param x2 x value of the upper right point of the given rectangle
  * @param y2 y value of the upper right point of the given rectangle
  * @param value a value to store in the r-tree
  * @tparam A a type of th value being put in the tree
  */
final case class RTreeEntry[A] private[core] (x1: Float, y1: Float, x2: Float, y2: Float, value: A) extends RTree[A] {
  def isEmpty: Boolean = false

  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, RTreeEntry[A]) => Float)(implicit distCalc: DistanceCalculator): Float = {
    val dist = distCalc.distance(x, y, this)
    if (dist < maxDist) f(dist, this)
    else maxDist
  }

  def search(x: Float, y: Float)(f: RTreeEntry[A] => Unit): Unit =
    if (this.y1 <= y && y <= this.y2 && this.x1 <= x && x <= this.x2) f(this)

  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Unit): Unit =
    if (this.y1 <= y2 && y1 <= this.y2 && this.x1 <= x2 && x1 <= this.x2) f(this)

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder =
    RTree.appendSpaces(sb, indent).append("RTreeEntry(").append(x1).append(',').append(y1).append(',')
      .append(x2).append(',').append(y2).append(',').append(value).append(")\n")
}

private final case class RTreeNode[A](x1: Float, y1: Float, x2: Float, y2: Float,
                                      level: Array[RTree[A]], from: Int, to: Int) extends RTree[A] {
  def isEmpty: Boolean = false

  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, RTreeEntry[A]) => Float)(implicit distCalc: DistanceCalculator): Float = {
    var minDist = maxDist
    val n = to - from
    var i = 0
    if (level(from).isInstanceOf[RTreeEntry[A]]) {
      while (i < n) {
        val e = level(from + i).asInstanceOf[RTreeEntry[A]]
        val d = distCalc.distance(x, y, e)
        if (d < minDist) minDist = f(d, e)
        i += 1
      }
    } else {
      val ps = new Array[Long](n)
      while (i < n) {
        ps(i) = (from + i) | (floatToRawIntBits(distCalc.distance(x, y, level(from + i))).toLong << 32)
        i += 1
      }
      java.util.Arrays.sort(ps) // Assuming that there no NaNs or negative values for distances
      i = 0
      while (i < n && {
        intBitsToFloat((ps(i) >> 32).toInt) < minDist && {
          minDist = level((ps(i) & 0x7fffffff).toInt).nearest(x, y, minDist)(f)
          true
        }
      }) i += 1
    }
    minDist
  }

  def search(x: Float, y: Float)(f: RTreeEntry[A] => Unit): Unit =
    if (this.y1 <= y && y <= this.y2 && this.x1 <= x && x <= this.x2) {
      var i = from
      while (i < to) {
        level(i).search(x, y)(f)
        i += 1
      }
    }

  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Unit): Unit =
    if (this.y1 <= y2 && y1 <= this.y2 && this.x1 <= x2 && x1 <= this.x2) {
      var i = from
      while (i < to) {
        level(i).search(x1, y1, x2, y2)(f)
        i += 1
      }
    }

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder = {
    RTree.appendSpaces(sb, indent).append("RTreeNode(").append(x1).append(',').append(y1).append(',')
      .append(x2).append(',').append(y2).append(")\n")
    var i = from
    while (i < to) {
      level(i).pretty(sb, indent + 2)
      i += 1
    }
    sb
  }

  override def equals(that: Any): Boolean = throw new UnsupportedOperationException("RTreeNode.equals")

  override def hashCode(): Int = throw new UnsupportedOperationException("RTreeNode.hashCode")
}

/**
  * A binary heap collection of distance/entry pairs.
  *
  * @param maxDist an initial limit of distance
  * @param maxSize a maximum size of the heap
  * @tparam A a type of th value being put in entries
  */
class RTreeEntryBinaryHeap[A](private[this] val maxDist: Float, private[this] val maxSize: Int) {
  private[this] var size0 = 0
  private[this] var distances = new Array[Float](8)
  private[this] var entries = new Array[RTreeEntry[A]](8)

  def put(d: Float, e: RTreeEntry[A]): Float = {
    var distances = this.distances
    var entries = this.entries
    if (size0 < maxSize) {
      size0 += 1
      if (size0 >= distances.length) {
        val newSize = Math.min(distances.length << 1, maxSize + 1)
        distances = java.util.Arrays.copyOf(distances, newSize)
        entries = java.util.Arrays.copyOf(entries, newSize)
        this.distances = distances
        this.entries = entries
      }
      var i = size0
      var j = i >> 1
      while (j > 0 && d >= distances(j)) {
        distances(i) = distances(j)
        entries(i) = entries(j)
        i = j
        j >>= 1
      }
      distances(i) = d
      entries(i) = e
      if (size0 == maxSize) distances(1) else maxDist
    } else if (size0 > 0 && d < distances(1)) {
      var i = 1
      var j = i << 1
      var k = j + 1
      if (k <= size0 && distances(k) >= distances(j)) j = k
      while (j <= size0 && distances(j) >= d) {
        distances(i) = distances(j)
        entries(i) = entries(j)
        i = j
        j = i << 1
        k = j + 1
        if (k <= size0 && distances(k) >= distances(j)) j = k
      }
      distances(i) = d
      entries(i) = e
      distances(1)
    } else maxDist
  }

  def toIndexedSeq: IndexedSeq[RTreeEntry[A]] = new IndexedSeq[RTreeEntry[A]] {
    private[this] val size0 = RTreeEntryBinaryHeap.this.size0
    private[this] val array = RTreeEntryBinaryHeap.this.entries

    override def length: Int = size0

    override def apply(idx: Int): RTreeEntry[A] =
      if (idx < 0 || idx >= size0) throw new IndexOutOfBoundsException(idx.toString)
      else array(idx + 1)
  }
}

private class DejaVuCounter {
  private[this] var n: Int = _

  def inc(): Unit = n += 1

  def decIfPositive(): Boolean =
    if (n > 0) {
      n -= 1
      false
    } else true
}
