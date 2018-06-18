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
  def apply[A](entries: Traversable[RTreeEntry[A]], nodeCapacity: Int = 16): RTree[A] = {
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
  def update[A](rtree: RTree[A], remove: Traversable[RTreeEntry[A]] = Nil, insert: Traversable[RTreeEntry[A]] = Nil,
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
      val cs = new mutable.OpenHashMap[RTree[A], DejaVuCounter](max(8, remove.size))
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
    * Returns a sequence of all entries in the R-tree whose MBR contains the given point.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @return a sequence of found values
    */
  def searchAll(x: Float, y: Float): Seq[RTreeEntry[A]] =
    new mutable.ResizableArray[RTreeEntry[A]] {
      search(x, y) { v =>
        if (size0 + 1 >= array.length) array = java.util.Arrays.copyOf(array, size0 << 1)
        array(size0) = v
        size0 += 1
        false
      }
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
  def searchAll(x1: Float, y1: Float, x2: Float, y2: Float): Seq[RTreeEntry[A]] =
    new mutable.ResizableArray[RTreeEntry[A]] {
      search(x1, y1, x2, y2) { v =>
        if (size0 + 1 >= array.length) array = java.util.Arrays.copyOf(array, size0 << 1)
        array(size0) = v
        size0 += 1
        false
      }
    }

  /**
    * Returns an option of of the nearest R-tree entry and the distance to it for the given point
    * and a specified distance calculator.
    *
    * Search distance can be limited by `maxDist` parameter.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param maxDist an exclusive limit of the distance (infinity by default)
    * @return an found option of the nearest entry and the distance to it
    */
  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (implicit distCalc: DistanceCalculator): Option[(Float, RTreeEntry[A])]

  /**
    * Call the provided `f` function with entries whose MBR contains the given point,
    * until the function returns true.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param f the function with receives found values and returns flag for fast exit
    * @return `true` if fast exit was requested by the provided function
    */
  def search(x: Float, y: Float)(f: RTreeEntry[A] => Boolean): Boolean

  /**
    * Call the provided `f` function with entries whose MBR intersects with the given point,
    * until the function returns true.
    *
    * @param x1 x value of the lower left point of the given rectangle
    * @param y1 y value of the lower left point of the given rectangle
    * @param x2 x value of the upper right point of the given rectangle
    * @param y2 y value of the upper right point of the given rectangle
    * @param f the function with receives found values and returns flag for fast exit
    * @return `true` if fast exit was requested by the provided function
    */
  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Boolean): Boolean

  /**
    * @return a sequence of all entries
    */
  def entries: Seq[RTreeEntry[A]] = new IndexedSeq[RTreeEntry[A]] {
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
  def x1: Float = throw new UnsupportedOperationException

  def y1: Float = throw new UnsupportedOperationException

  def x2: Float = throw new UnsupportedOperationException

  def y2: Float = throw new UnsupportedOperationException

  def isEmpty: Boolean = true

  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (implicit distCalc: DistanceCalculator): Option[(Float, RTreeEntry[A])] = None

  def search(x: Float, y: Float)(f: RTreeEntry[A] => Boolean): Boolean = false

  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Boolean): Boolean = false

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
final case class RTreeEntry[A] (x1: Float, y1: Float, x2: Float, y2: Float, value: A) extends RTree[A] {
  if (!(x2 >= x1)) throw new IllegalArgumentException("x2 should be greater than x1 and any of them should not be NaN")
  if (!(y2 >= y1)) throw new IllegalArgumentException("y2 should be greater than y1 and any of them should not be NaN")

  def isEmpty: Boolean = false

  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (implicit distCalc: DistanceCalculator): Option[(Float, RTreeEntry[A])] = {
    val dist = distCalc.distance(x, y, this)
    if (dist <= maxDist) Some((dist, this))
    else None
  }

  def search(x: Float, y: Float)(f: RTreeEntry[A] => Boolean): Boolean =
    this.y1 <= y && y <= this.y2 && this.x1 <= x && x <= this.x2 && f(this)

  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Boolean): Boolean =
    this.y1 <= y2 && y1 <= this.y2 && this.x1 <= x2 && x1 <= this.x2 && f(this)

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder =
    RTree.appendSpaces(sb, indent).append("RTreeEntry(").append(x1).append(',').append(y1).append(',')
      .append(x2).append(',').append(y2).append(',').append(value).append(")\n")
}

object RTreeEntry {
  /**
    * Create an entry for a point and a value.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def apply[A](x: Float, y: Float, value: A): RTreeEntry[A] = {
    if (x != x) throw new IllegalArgumentException("x should not be NaN")
    if (y != y) throw new IllegalArgumentException("y should not be NaN")
    new RTreeEntry[A](x, y, x, y, value)
  }
}

private final case class RTreeNode[A](x1: Float, y1: Float, x2: Float, y2: Float,
                                      level: Array[RTree[A]], from: Int, to: Int) extends RTree[A] {
  def isEmpty: Boolean = false

  def nearest(x: Float, y: Float, maxDist: Float = Float.PositiveInfinity)
             (implicit distCalc: DistanceCalculator): Option[(Float, RTreeEntry[A])] = {
    var minDist = maxDist
    val n = to - from
    var i = 0
    if (level(from).isInstanceOf[RTreeEntry[A]]) {
      var re: RTreeEntry[A] = null
      while (i < n) {
        val e = level(from + i).asInstanceOf[RTreeEntry[A]]
        val d = distCalc.distance(x, y, e)
        if (d < minDist) {
          minDist = d
          re = e
        }
        i += 1
      }
      if (re eq null) None else Some((minDist, re))
    } else {
      val ps = new Array[Long](n)
      while (i < n) {
        ps(i) = (from + i) | (floatToRawIntBits(distCalc.distance(x, y, level(from + i))).toLong << 32)
        i += 1
      }
      java.util.Arrays.sort(ps)
      i = 0
      var result: Option[(Float, RTreeEntry[A])] = None
      while (i < n && {
        val d = intBitsToFloat((ps(i) >> 32).toInt)
        d < minDist && {
          val r = level(ps(i).toInt).nearest(x, y, minDist)
          if (r.isDefined) {
            minDist = r.get._1
            result = r
          }
          true
        }
      }) i += 1
      result
    }
  }

  def search(x: Float, y: Float)(f: RTreeEntry[A] => Boolean): Boolean =
    this.y1 <= y && y <= this.y2 && this.x1 <= x && x <= this.x2 && {
      var i = from
      while (i < to) {
        if (level(i).search(x, y)(f)) return true
        i += 1
      }
      false
    }

  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Boolean): Boolean =
    this.y1 <= y2 && y1 <= this.y2 && this.x1 <= x2 && x1 <= this.x2 && {
      var i = from
      while (i < to) {
        if (level(i).search(x1, y1, x2, y2)(f)) return true
        i += 1
      }
      false
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

  override def equals(that: Any): Boolean = throw new UnsupportedOperationException

  override def hashCode(): Int = throw new UnsupportedOperationException
}

/**
  * A type class for distance calculations that can be used for `nearest` requests
  * to an R-tree instances.
  */
trait DistanceCalculator {
  /**
    * Returns a distance from the given point to a boinding box of the specified RTree.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param t an RTree instance
    * @tparam A a type of th value being put in the tree
    * @return return a distance value
    */
  def distance[A](x: Float, y: Float, t: RTree[A]): Float
}

object EuclideanDistanceCalculator {
  /**
    * An instance of the `DistanceCalculator` type class which use Euclidean geometry
    * to calculate distances.
    */
  implicit val calculator: DistanceCalculator = new DistanceCalculator {
    override def distance[A](x: Float, y: Float, t: RTree[A]): Float = {
      val dy = if (y < t.y1) t.y1 - y else if (y < t.y2) 0 else y - t.y2
      val dx = if (x < t.x1) t.x1 - x else if (x < t.x2) 0 else x - t.x2
      if (dy == 0) dx
      else if (dx == 0) dy
      else sqrt(dx * dx + dy * dy).toFloat
    }
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