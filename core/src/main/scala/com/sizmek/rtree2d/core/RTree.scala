package com.sizmek.rtree2d.core

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
        if (nodeCount > sliceCapacity) util.Arrays.sort(level, i, sliceTo, yComp)
        do {
          val packTo = min(i + nodeCapacity, sliceTo)
          nextLevel(j) = packNode(level, i, packTo)
          j += 1
          i = packTo
        } while (i < sliceTo)
      } while (j < nodeCount)
      pack(nextLevel, nodeCapacity, xComp, yComp)
    }
  }

  private[this] def packNode[A](level: Array[RTree[A]], from: Int, to: Int): RTree[A] = {
    var t = level(from)
    var i = from + 1
    if (i == to) t
    else {
      var x1 = t.x1
      var y1 = t.y1
      var x2 = t.x2
      var y2 = t.y2
      do {
        t = level(i)
        if (x1 > t.x1) x1 = t.x1
        if (y1 > t.y1) y1 = t.y1
        if (x2 < t.x2) x2 = t.x2
        if (y2 < t.y2) y2 = t.y2
        i += 1
      } while (i < to)
      new RTreeNode(x1, y1, x2, y2, level, from, to)
    }
  }

  private[this] def xComparator[A]: Comparator[RTree[A]] = new Comparator[RTree[A]] {
    override def compare(t1: RTree[A], t2: RTree[A]): Int =
      java.lang.Float.floatToRawIntBits((t1.x1 + t1.x2) - (t2.x1 + t2.x2))
  }

  private[this] def yComparator[A]: Comparator[RTree[A]] = new Comparator[RTree[A]] {
    override def compare(t1: RTree[A], t2: RTree[A]): Int =
      java.lang.Float.floatToRawIntBits((t1.y1 + t1.y2) - (t2.y1 + t2.y2))
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
        array(size0) = v.asInstanceOf[AnyRef]
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
        array(size0) = v.asInstanceOf[AnyRef]
        size0 += 1
        false
      }
    }

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
    private[this] val ts: Array[RTree[A]] = RTree.lastLevel(RTree.this)

    override def length: Int = ts.length

    override def apply(idx: Int): RTreeEntry[A] = ts(idx).asInstanceOf[RTreeEntry[A]]
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

  def search(x: Float, y: Float)(f: RTreeEntry[A] => Boolean): Boolean =
    this.x1 <= x && x <= this.x2 && this.y1 <= y && y <= this.y2 && f(this)

  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Boolean): Boolean =
    this.x1 <= x2 && x1 <= this.x2 && this.y1 <= y2 && y1 <= this.y2 && f(this)

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
  def search(x: Float, y: Float)(f: RTreeEntry[A] => Boolean): Boolean =
    this.x1 <= x && x <= this.x2 && this.y1 <= y && y <= this.y2 && {
      val ts = level
      val l = to
      var i = from
      do {
        if (ts(i).search(x, y)(f)) return true
        i += 1
      } while (i < l)
      false
    }

  def search(x1: Float, y1: Float, x2: Float, y2: Float)(f: RTreeEntry[A] => Boolean): Boolean =
    this.x1 <= x2 && x1 <= this.x2 && this.y1 <= y2 && y1 <= this.y2 && {
      val ts = level
      val l = to
      var i = from
      do {
        if (ts(i).search(x1, y1, x2, y2)(f)) return true
        i += 1
      } while (i < l)
      false
    }

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder = {
    RTree.appendSpaces(sb, indent).append("RTreeNode(").append(x1).append(',').append(y1).append(',')
      .append(x2).append(',').append(y2).append(")\n")
    val ts = level
    val l = to
    var i = from
    do {
      ts(i).pretty(sb, indent + 2)
      i += 1
    } while (i < l)
    sb
  }

  override def equals(that: Any): Boolean = throw new UnsupportedOperationException

  override def hashCode(): Int = throw new UnsupportedOperationException
}
