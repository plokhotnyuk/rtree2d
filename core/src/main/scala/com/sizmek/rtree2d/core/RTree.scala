package com.sizmek.rtree2d.core

import java.lang.Float.{floatToRawIntBits, intBitsToFloat}
import java.lang.Math._
import java.util
import java.util.Comparator

import scala.annotation.tailrec
import scala.collection.mutable

object RTree {
  /**
    * Create an entry for a point and a value.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def entry[A](x: Float, y: Float, value: A): RTreeEntry[A] = {
    if (x != x) throw new IllegalArgumentException("x should not be NaN")
    if (y != y) throw new IllegalArgumentException("y should not be NaN")
    new RTreeEntry[A](x, y, x, y, value)
  }

  /**
    * Create an entry for a rectangle and a value.
    *
    * @param minX x coordinate of the left bottom corner
    * @param minY y coordinate of the left bottom corner
    * @param maxX x coordinate of the right top corner
    * @param maxY y coordinate of the right top corner
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def entry[A](minX: Float, minY: Float, maxX: Float, maxY: Float, value: A): RTreeEntry[A] = {
    if (!(maxX >= minX))
      throw new IllegalArgumentException("maxX should be greater than minX and any of them should not be NaN")
    if (!(maxY >= minY))
      throw new IllegalArgumentException("maxY should be greater than minY and any of them should not be NaN")
    new RTreeEntry[A](minX, minY, maxX, maxY, value)
  }

  /**
    * Create an entry specified by center point with distance and a value.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param distance a value of distance to edges of the entry bounding box
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def entry[A](x: Float, y: Float, distance: Float, value: A): RTreeEntry[A] = {
    if (x != x) throw new IllegalArgumentException("x should not be NaN")
    if (y != y) throw new IllegalArgumentException("y should not be NaN")
    if (!(distance >= 0)) throw new IllegalArgumentException("distance should not be less than 0 or NaN")
    new RTreeEntry[A](x - distance, y - distance, x + distance, y + distance, value)
  }

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

  private[core] def distance[A](x: Float, y: Float, t: RTree[A]): Float = {
    val dy = if (y < t.minY) t.minY - y else if (y < t.maxY) 0 else y - t.maxY
    val dx = if (x < t.minX) t.minX - x else if (x < t.maxX) 0 else x - t.maxX
    if (dy == 0) dx
    else if (dx == 0) dy
    else sqrt(dx * dx + dy * dy).toFloat
  }

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
      var minY = t.minY
      var maxY = t.maxY
      var minX = t.minX
      var maxX = t.maxX
      do {
        t = level(i)
        if (minY > t.minY) minY = t.minY
        if (maxY < t.maxY) maxY = t.maxY
        if (minX > t.minX) minX = t.minX
        if (maxX < t.maxX) maxX = t.maxX
        i += 1
      } while (i < to)
      new RTreeNode(minX, minY, maxX, maxY, level, from, to)
    }
  }

  private[this] def xComparator[A]: Comparator[RTree[A]] = new Comparator[RTree[A]] {
    override def compare(t1: RTree[A], t2: RTree[A]): Int = floatToRawIntBits((t1.minX + t1.maxX) - (t2.minX + t2.maxX))
  }

  private[this] def yComparator[A]: Comparator[RTree[A]] = new Comparator[RTree[A]] {
    override def compare(t1: RTree[A], t2: RTree[A]): Int = floatToRawIntBits((t1.minY + t1.maxY) - (t2.minY + t2.maxY))
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
  def minX: Float

  /**
    * @return y value of the lower left point of the MBR
    */
  def minY: Float

  /**
    * @return x value of the upper right point of the MBR
    */
  def maxX: Float

  /**
    * @return y value of the upper right point of the MBR
    */
  def maxY: Float

  def isEmpty: Boolean

  /**
    * Returns an option of the nearest R-tree entry for the given point.
    *
    * Search distance can be limited by the `maxDist` parameter.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param maxDist an exclusive limit of the distance (infinity by default)
    * @return an option of the nearest entry
    */
  def nearestOption(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity): Option[RTreeEntry[A]] = {
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
    * @return a sequence of the nearest entries
    */
  def nearestK(x: Float, y: Float, k: Int, maxDist: Float = Float.PositiveInfinity): IndexedSeq[RTreeEntry[A]] =
    if (k <= 0) Vector.empty
    else new DistanceHeap[RTreeEntry[A]](maxDist, k) {
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
    * @return the distance to a found entry or initially submitted value of maxDist
    */
  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)(f: (Float, RTreeEntry[A]) => Float): Float

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
    * @param minX x value of the lower left point of the given rectangle
    * @param minY y value of the lower left point of the given rectangle
    * @param maxX x value of the upper right point of the given rectangle
    * @param maxY y value of the upper right point of the given rectangle
    * @param f the function that receives found values
    */
  def search(minX: Float, minY: Float, maxX: Float, maxY: Float)(f: RTreeEntry[A] => Unit): Unit

  /**
    * Returns a sequence of all entries in the R-tree whose MBR contains the given point.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @return a sequence of found values
    */
  def searchAll(x: Float, y: Float): IndexedSeq[RTreeEntry[A]] = {
    var size: Int = 0
    var array: Array[RTreeEntry[A]] = new Array[RTreeEntry[A]](16)
    search(x, y) { v =>
      if (size + 1 >= array.length) array = java.util.Arrays.copyOf(array, size << 1)
      array(size) = v
      size += 1
    }
    new FixedIndexedSeq[RTreeEntry[A]](array, size)
  }

  /**
    * Returns a sequence of all entries in the R-tree whose MBR intersects the given rectangle.
    *
    * @param minX x value of the lower left point of the given rectangle
    * @param minY y value of the lower left point of the given rectangle
    * @param maxX x value of the upper right point of the given rectangle
    * @param maxY y value of the upper right point of the given rectangle
    * @return a sequence of found values
    */
  def searchAll(minX: Float, minY: Float, maxX: Float, maxY: Float): IndexedSeq[RTreeEntry[A]] = {
    var size: Int = 0
    var array: Array[RTreeEntry[A]] = new Array[RTreeEntry[A]](16)
    search(minX, minY, maxX, maxY) { v =>
      if (size + 1 >= array.length) array = java.util.Arrays.copyOf(array, size << 1)
      array(size) = v
      size += 1
    }
    new FixedIndexedSeq[RTreeEntry[A]](array, size)
  }

  /**
    * @return a sequence of all entries
    */
  def entries: IndexedSeq[RTreeEntry[A]] = new AdaptedIndexedSeq[RTree[A], RTreeEntry[A]](RTree.lastLevel(RTree.this))

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
  def minX: Float = throw new UnsupportedOperationException("RTreeNil.minX")

  def minY: Float = throw new UnsupportedOperationException("RTreeNil.minY")

  def maxX: Float = throw new UnsupportedOperationException("RTreeNil.maxX")

  def maxY: Float = throw new UnsupportedOperationException("RTreeNil.maxY")

  def isEmpty: Boolean = true

  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, RTreeEntry[A]) => Float): Float = maxDist

  def search(x: Float, y: Float)(f: RTreeEntry[A] => Unit): Unit = ()

  def search(minX: Float, minY: Float, maxX: Float, maxY: Float)(f: RTreeEntry[A] => Unit): Unit = ()

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder =
    RTree.appendSpaces(sb, indent).append("RTreeNil()\n")
}

/**
  * Create an entry for a rectangle and a value.
  *
  * @param minX x value of the lower left point of the given rectangle
  * @param minY y value of the lower left point of the given rectangle
  * @param maxX x value of the upper right point of the given rectangle
  * @param maxY y value of the upper right point of the given rectangle
  * @param value a value to store in the r-tree
  * @tparam A a type of th value being put in the tree
  */
final case class RTreeEntry[A] private[core] (minX: Float, minY: Float, maxX: Float, maxY: Float,
                                              value: A) extends RTree[A] {
  def isEmpty: Boolean = false

  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, RTreeEntry[A]) => Float): Float = {
    val dist = RTree.distance(x, y, this)
    if (dist < maxDist) f(dist, this)
    else maxDist
  }

  def search(x: Float, y: Float)(f: RTreeEntry[A] => Unit): Unit =
    if (this.minY <= y && y <= this.maxY && this.minX <= x && x <= this.maxX) f(this)

  def search(minX: Float, minY: Float, maxX: Float, maxY: Float)(f: RTreeEntry[A] => Unit): Unit =
    if (this.minY <= maxY && minY <= this.maxY && this.minX <= maxX && minX <= this.maxX) f(this)

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder =
    RTree.appendSpaces(sb, indent).append("RTreeEntry(").append(minX).append(',').append(minY).append(',')
      .append(maxX).append(',').append(maxY).append(',').append(value).append(")\n")
}

private final case class RTreeNode[A](minX: Float, minY: Float, maxX: Float, maxY: Float,
                                      level: Array[RTree[A]], from: Int, to: Int) extends RTree[A] {
  def isEmpty: Boolean = false

  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, RTreeEntry[A]) => Float): Float = {
    var minDist = maxDist
    val n = to - from
    var i = 0
    if (level(from).isInstanceOf[RTreeEntry[A]]) {
      while (i < n) {
        val e = level(from + i).asInstanceOf[RTreeEntry[A]]
        val d = RTree.distance(x, y, e)
        if (d < minDist) minDist = f(d, e)
        i += 1
      }
    } else {
      val ps = new Array[Long](n)
      while (i < n) {
        ps(i) = (from + i) | (floatToRawIntBits(RTree.distance(x, y, level(from + i))).toLong << 32)
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
    if (this.minY <= y && y <= this.maxY && this.minX <= x && x <= this.maxX) {
      var i = from
      while (i < to) {
        level(i).search(x, y)(f)
        i += 1
      }
    }

  def search(minX: Float, minY: Float, maxX: Float, maxY: Float)(f: RTreeEntry[A] => Unit): Unit =
    if (this.minY <= maxY && minY <= this.maxY && this.minX <= maxX && minX <= this.maxX) {
      var i = from
      while (i < to) {
        level(i).search(minX, minY, maxX, maxY)(f)
        i += 1
      }
    }

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder = {
    RTree.appendSpaces(sb, indent).append("RTreeNode(").append(minX).append(',').append(minY).append(',')
      .append(maxX).append(',').append(maxY).append(")\n")
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
