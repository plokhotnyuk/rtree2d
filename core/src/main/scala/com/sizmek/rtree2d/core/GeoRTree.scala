package com.sizmek.rtree2d.core

import java.lang.Float.{floatToRawIntBits, intBitsToFloat}
import java.lang.Math._
import java.util
import java.util.Comparator

import scala.annotation.tailrec
import scala.collection.mutable

object GeoRTree {
  private[core] val radPerDegree = PI / 180

  private[this] val degreePerRad = 180 / PI

  /**
    * 6371.0088 is a value of the mean radius in kilometers, see: https://en.wikipedia.org/wiki/Earth_radius#Mean_radius
    * It allows to get +0.2% accuracy on poles, -0.1% on the equator, and less than ±0.05% on medium latitudes.
    * Precision of 32-bit float number allows to locate points and calculate distances with an error ±0.5 meters.
    */
  private[this] val earthMeanRadius = 6371.0088

  private[this] val circumference = earthMeanRadius * radPerDegree

  /**
    * Create an entry specified by a point with spherical coordinates and a value.
    *
    * @param lat a latitude coordinate of the given point
    * @param lon a latitude coordinate of the given point
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def entry[A](lat: Float, lon: Float, value: A): GeoRTreeEntry[A] = {
    if (!(lat >= -90 && lat <= 90))
      throw new IllegalArgumentException("lat should not be out of range from -90 to 90 or NaN")
    if (!(lon >= -180 && lon <= 180))
      throw new IllegalArgumentException("lon should not be out of range from -180 to 180 or NaN")
    val sinLat = sin(lat * radPerDegree)
    val cosLat = cos(lat * radPerDegree)
    new GeoRTreeEntry[A](lat, lon, lat, lon, value, sinLat, cosLat, sinLat, cosLat)
  }

  /**
    * Create an entry specified by a rectangle with spherical coordinates and a value.
    *
    * @param minLat a latitude coordinate of the left bottom corner
    * @param minLon a latitude coordinate of the left bottom corner
    * @param maxLat a latitude coordinate of the right top corner
    * @param maxLon a latitude coordinate of the right top corner
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def entry[A](minLat: Float, minLon: Float, maxLat: Float, maxLon: Float, value: A): GeoRTreeEntry[A] = {
    if (!(minLat >= -90)) throw new IllegalArgumentException("minLat should not be less than -90 or NaN")
    if (!(minLon >= -180)) throw new IllegalArgumentException("minLon should not be less than -180 or NaN")
    if (!(maxLat <= 90 && maxLat >= minLat))
      throw new IllegalArgumentException("maxLat should not be greater than 90 or less than minLat or NaN")
    if (!(maxLon <= 180 && maxLon >= minLon))
      throw new IllegalArgumentException("maxLon should not be greater than 180 or less than minLat  or NaN")
    val radMinLat = minLat * radPerDegree
    val radMaxLat = maxLat * radPerDegree
    new GeoRTreeEntry[A](minLat, minLon, maxLat, maxLon, value,
      sin(radMinLat), cos(radMinLat), sin(radMaxLat), cos(radMaxLat))
  }

  /**
    * Create an indexed sequence of entries that are specified by a circular area on the Earth and a value.
    *
    * Sequence of entries required for case when the circle is crossed by the anti-meridian because the GeoRTree which
    * use bounding box form longitudes and latitudes for geo-indexing doesn't support wrapping of geo-coordinates over
    * the Earth, so we split that entries on two by the anti-meridian.
    *
    * Used formula with description is here: http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates#Longitude
    *
    * @param lat a latitude coordinate of the given center point
    * @param lon a latitude coordinate of the given center point
    * @param distance a distance, from the center point to borders of the circular area on the Earth surface (in km),
    *                 if the value of distance is greater than a half of circumference of the Earth then a whole sphere
    *                 will be bounded
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def entries[A](lat: Float, lon: Float, distance: Float, value: A): IndexedSeq[GeoRTreeEntry[A]] = {
    if (!(lat >= -90 && lat <= 90))
      throw new IllegalArgumentException("lat should not be out of range from -90 to 90 or NaN")
    if (!(lon >= -180 && lon <= 180))
      throw new IllegalArgumentException("lon should not be out of range from -180 to 180 or NaN")
    if (!(distance >= 0)) throw new IllegalArgumentException("distance should not be less than 0 or NaN")
    val radLon = lon * radPerDegree
    val radLat = lat * radPerDegree
    val radDeltaLat = distance / earthMeanRadius
    val radMinLat = radLat - radDeltaLat
    val radMaxLat = radLat + radDeltaLat
    var minLat = (radMinLat * degreePerRad).toFloat
    var maxLat = (radMaxLat * degreePerRad).toFloat
    if (minLat > -90 && maxLat < 90) {
      val deltaLon = asin(sin(radDeltaLat) / cos(radLat))
      val minLon = ((radLon - deltaLon) * degreePerRad).toFloat
      val maxLon = ((radLon + deltaLon) * degreePerRad).toFloat
      val sinMinLat = sin(radMinLat)
      val cosMinLat = cos(radMinLat)
      val sinMaxLat = sin(radMaxLat)
      val cosMaxLat = cos(radMaxLat)
      if (minLon < -180) {
        new IndexedSeq2(new GeoRTreeEntry(minLat, -180, maxLat, maxLon, value,
          sinMinLat, cosMinLat, sinMaxLat, cosMaxLat),
          new GeoRTreeEntry(minLat, minLon + 360, maxLat, 180, value,
            sinMinLat, cosMinLat, sinMaxLat, cosMaxLat))
      } else if (maxLon > 180) {
        new IndexedSeq2(new GeoRTreeEntry(minLat, -180, maxLat, maxLon - 360, value,
          sinMinLat, cosMinLat, sinMaxLat, cosMaxLat),
          new GeoRTreeEntry(minLat, minLon, maxLat, 180, value,
            sinMinLat, cosMinLat, sinMaxLat, cosMaxLat))
      } else new IndexedSeq1(new GeoRTreeEntry(minLat, minLon, maxLat, maxLon, value,
        sinMinLat, cosMinLat, sinMaxLat, cosMaxLat))
    } else {
      var sinMinLat = 0.0
      var cosMinLat = 0.0
      var sinMaxLat = 0.0
      var cosMaxLat = 0.0
      if (minLat <= -90) {
        minLat = -90
        sinMinLat = -1
        cosMinLat = 0
      } else {
        sinMinLat = sin(radMinLat)
        cosMinLat = cos(radMinLat)
      }
      if (maxLat >= 90) {
        maxLat = 90
        sinMaxLat = 1
        cosMaxLat = 0
      } else {
        sinMaxLat = sin(radMaxLat)
        cosMaxLat = cos(radMaxLat)
      }
      new IndexedSeq1(new GeoRTreeEntry(minLat, -180, maxLat, 180, value, sinMinLat, cosMinLat, sinMaxLat, cosMaxLat))
    }
  }

  /**
    * Construct an GeoGeoRTree from a sequence of entries using STR packing.
    *
    * @param entries the sequence of entries
    * @param nodeCapacity the maximum number of children nodes (16 by default)
    * @tparam A a type of values being put in the tree
    * @return an GeoRTree instance
    */
  def apply[A](entries: Iterable[GeoRTreeEntry[A]], nodeCapacity: Int = 16): GeoRTree[A] = {
    if (nodeCapacity <= 1) throw new IllegalArgumentException("nodeCapacity should be greater than 1")
    pack(entries.toArray[GeoRTree[A]], nodeCapacity, xComparator[A], yComparator[A])
  }

  /**
    * Update an GeoRTree by withdrawing matched entries specified by the `remove` argument and adding entries from the
    * `insert` argument, than repack resulting entries to a new GeoRTree using STR packing.
    *
    * @param rtree the GeoRTree
    * @param remove the sequence of entries to remove
    * @param insert the sequence of entries to insert
    * @param nodeCapacity the maximum number of children nodes (16 by default)
    * @tparam A a type of values being put in the tree
    * @return an GeoRTree instance
    */
  def update[A](rtree: GeoRTree[A], remove: Iterable[GeoRTreeEntry[A]] = Nil, insert: Iterable[GeoRTreeEntry[A]] = Nil,
                nodeCapacity: Int = 16): GeoRTree[A] =
    if ((rtree.isEmpty || remove.isEmpty) && insert.isEmpty) rtree
    else if (rtree.isEmpty && remove.isEmpty) {
      pack(insert.toArray[GeoRTree[A]], nodeCapacity, xComparator[A], yComparator[A])
    } else if (remove.isEmpty) {
      val es1 = GeoRTree.lastLevel(rtree)
      val l1 = es1.length
      val es = util.Arrays.copyOf(es1, l1 + insert.size)
      insert.copyToArray(es, l1)
      pack(es, nodeCapacity, xComparator[A], yComparator[A])
    } else {
      val cs = new mutable.AnyRefMap[GeoRTree[A], DejaVuCounter](remove.size)
      remove.foreach(e => cs.getOrElseUpdate(e, new DejaVuCounter).inc())
      val es1 = GeoRTree.lastLevel(rtree)
      val l1 = es1.length
      var n = insert.size
      val es = new Array[GeoRTree[A]](l1 + n)
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
  private[core] def lastLevel[A](t: GeoRTree[A]): Array[GeoRTree[A]] = t match {
    case tn: GeoRTreeNode[A] => tn.level(0) match {
      case _: GeoRTreeEntry[A] => tn.level
      case n => lastLevel(n)
    }
    case e: GeoRTreeEntry[A] => Array[GeoRTree[A]](e)
    case _ => new Array[GeoRTree[A]](0)
  }

  @tailrec
  private[core] def appendSpaces(sb: java.lang.StringBuilder, n: Int): java.lang.StringBuilder =
    if (n > 0) appendSpaces(sb.append(' '), n - 1)
    else sb

  /**
    * Use the haversine formula to determine the great-circle distance between a point and a rounding box of R-tree on
    * a sphere with specified radius given their longitudes and latitudes.
    *
    * Calculations was borrowed from the geoflatbush project of Vladimir Agafonkin:
    * https://github.com/mourner/geoflatbush/blob/master/index.mjs
    */
  private[core] def distance[A](lat: Float, lon: Float, sinLat: Double, cosLat: Double, t: GeoRTree[A]): Float = {
    val minLon = t.minLon
    val maxLon = t.maxLon
    val minLat = t.minLat
    val maxLat = t.maxLat
    if (lon >= minLon && lon <= maxLon) {
      if (lat < minLat) ((minLat - lat) * circumference).toFloat
      else if (lat > maxLat) ((lat - maxLat) * circumference).toFloat
      else 0
    } else (acos {
      if (minLon == maxLon && minLat == maxLat) {
        min(sinLat * t.sinMinLat + cosLat * t.cosMinLat * cos((minLon - lon) * radPerDegree), 1)
      } else {
        val cosLonDelta = cos(min(normalize(minLon - lon), normalize(lon - maxLon)) * radPerDegree)
        val d = max(
          min(sinLat * t.sinMinLat + cosLat * t.cosMinLat * cosLonDelta, 1),
          min(sinLat * t.sinMaxLat + cosLat * t.cosMaxLat * cosLonDelta, 1))
        val radExtremumLat = atan(sinLat / (cosLat * cosLonDelta))
        val extremumLat = radExtremumLat * degreePerRad
        if (extremumLat <= minLat || extremumLat >= maxLat) d
        else max(d, min(sinLat * sin(radExtremumLat) + cosLat * cos(radExtremumLat) * cosLonDelta, 1))
      }
    } * earthMeanRadius).toFloat
  }

  private[this] def normalize(lonDelta: Float): Float = if (lonDelta < 0) lonDelta + 360 else lonDelta

  @tailrec
  private[this] def pack[A](level: Array[GeoRTree[A]], nodeCapacity: Int, xComp: Comparator[GeoRTree[A]],
                            yComp: Comparator[GeoRTree[A]]): GeoRTree[A] = {
    val l = level.length
    if (l == 0) new GeoRTreeNil
    else if (l <= nodeCapacity) packNode(level, 0, l)
    else {
      util.Arrays.sort(level, xComp)
      val nodeCount = ceil(l.toFloat / nodeCapacity).toInt
      val sliceCapacity = ceil(sqrt(nodeCount)).toInt * nodeCapacity
      val nextLevel = new Array[GeoRTree[A]](nodeCount)
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

  private[this] def packNode[A](level: Array[GeoRTree[A]], from: Int, to: Int): GeoRTree[A] = {
    var t = level(from)
    var i = from + 1
    if (i == to) t
    else {
      var minLon = t.minLon
      var maxLon = t.maxLon
      var minLat = t.minLat
      var maxLat = t.maxLat
      do {
        t = level(i)
        if (minLon > t.minLon) minLon = t.minLon
        if (maxLon < t.maxLon) maxLon = t.maxLon
        if (minLat > t.minLat) minLat = t.minLat
        if (maxLat < t.maxLat) maxLat = t.maxLat
        i += 1
      } while (i < to)
      new GeoRTreeNode(minLat, minLon, maxLat, maxLon, level, from, to,
        sin(minLat * radPerDegree), cos(minLat * radPerDegree), sin(maxLat * radPerDegree), cos(maxLat * radPerDegree))
    }
  }

  private[this] def xComparator[A]: Comparator[GeoRTree[A]] = new Comparator[GeoRTree[A]] {
    override def compare(t1: GeoRTree[A], t2: GeoRTree[A]): Int =
      floatToRawIntBits((t1.minLat + t1.maxLat) - (t2.minLat + t2.maxLat))
  }

  private[this] def yComparator[A]: Comparator[GeoRTree[A]] = new Comparator[GeoRTree[A]] {
    override def compare(t1: GeoRTree[A], t2: GeoRTree[A]): Int =
      floatToRawIntBits((t1.minLon + t1.maxLon) - (t2.minLon + t2.maxLon))
  }
}

/**
  * In-memory immutable r-tree with the minimal bounding rectangle (MBR).
  *
  * @tparam A a type of values being put in the tree
  */
sealed trait GeoRTree[A] {
  /**
    * @return a latitude value of the lower left point of the MBR
    */
  def minLat: Float

  /**
    * @return a longitude value of the lower left point of the MBR
    */
  def minLon: Float

  /**
    * @return a latitude value of the upper right point of the MBR
    */
  def maxLat: Float

  /**
    * @return a longitude value of the upper right point of the MBR
    */
  def maxLon: Float

  def isEmpty: Boolean

  /**
    * Returns an option of the nearest R-tree entry for the given point.
    *
    * Search distance can be limited by the `maxDist` parameter.
    *
    * @param lat a latitude value of the given point
    * @param lon a longitude value of the given point
    * @param maxDist an exclusive limit of the distance (infinity by default)
    * @return an option of the nearest entry
    */
  def nearestOption(lat: Float, lon: Float, maxDist: Float = Float.PositiveInfinity): Option[GeoRTreeEntry[A]] = {
    var res: GeoRTreeEntry[A] = null
    nearest(lat, lon, sin(lat * GeoRTree.radPerDegree), cos(lat * GeoRTree.radPerDegree), maxDist)((d, e) => {
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
    * @param lat a latitude value of the given point
    * @param lon a longitude value of the given point
    * @param k a maximum number of nearest entries to collect
    * @param maxDist an exclusive limit of the distance (infinity by default)
    * @return a sequence of the nearest entries
    */
  def nearestK(lat: Float, lon: Float, k: Int, maxDist: Float = Float.PositiveInfinity): IndexedSeq[GeoRTreeEntry[A]] =
    if (k <= 0) Vector.empty
    else new DistanceHeap[GeoRTreeEntry[A]](maxDist, k) {
      nearest(lat, lon, sin(lat * GeoRTree.radPerDegree), cos(lat * GeoRTree.radPerDegree), maxDist)((d, e) => put(d, e))
    }.toIndexedSeq

  /**
    * Returns a distance to the nearest R-tree entry for the given point.
    *
    * Search distance can be limited by the `maxDist` parameter.
    *
    * @param lat a latitude value of the given point
    * @param lon a longitude value of the given point
    * @param maxDist an exclusive limit of the distance (infinity by default)
    * @param f the function that receives found values and their distances and returns the current maximum distance
    * @return the distance to a found entry or initially submitted value of maxDist
    */
  def nearest(lat: Float, lon: Float, sinLat: Double, cosLat: Double, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, GeoRTreeEntry[A]) => Float): Float

  /**
    * Call the provided `f` function with entries whose MBR contains the given point.
    *
    * @param lat a latitude value of the given point
    * @param lon a longitude value of the given point
    * @param f the function that receives found values
    */
  def search(lat: Float, lon: Float)(f: GeoRTreeEntry[A] => Unit): Unit

  /**
    * Call the provided `f` function with entries whose MBR intersects with the given point.
    *
    * @param minLat a latitude value of the lower left point of the given rectangle
    * @param minLon a longitude value of the lower left point of the given rectangle
    * @param maxLat a latitude value of the upper right point of the given rectangle
    * @param maxLon a longitude value of the upper right point of the given rectangle
    * @param f the function that receives found values
    */
  def search(minLat: Float, minLon: Float, maxLat: Float, maxLon: Float)(f: GeoRTreeEntry[A] => Unit): Unit

  /**
    * Returns a sequence of all entries in the R-tree whose MBR contains the given point.
    *
    * @param lat a latitude value of the given point
    * @param lon a longitude value of the given point
    * @return a sequence of found values
    */
  def searchAll(lat: Float, lon: Float): IndexedSeq[GeoRTreeEntry[A]] = {
    var size: Int = 0
    var array: Array[GeoRTreeEntry[A]] = new Array[GeoRTreeEntry[A]](16)
    search(lat, lon) { v =>
      if (size + 1 >= array.length) array = java.util.Arrays.copyOf(array, size << 1)
      array(size) = v
      size += 1
    }
    new FixedIndexedSeq[GeoRTreeEntry[A]](array, size)
  }

  /**
    * Returns a sequence of all entries in the R-tree whose MBR intersects the given rectangle.
    *
    * @param minLat a latitude value of the lower left point of the given rectangle
    * @param minLon a longitude value of the lower left point of the given rectangle
    * @param maxLat a latitude value of the upper right point of the given rectangle
    * @param maxLon a longitude value of the upper right point of the given rectangle
    * @return a sequence of found values
    */
  def searchAll(minLat: Float, minLon: Float, maxLat: Float, maxLon: Float): IndexedSeq[GeoRTreeEntry[A]] = {
    var size: Int = 0
    var array: Array[GeoRTreeEntry[A]] = new Array[GeoRTreeEntry[A]](16)
    search(minLat, minLon, maxLat, maxLon) { v =>
      if (size + 1 >= array.length) array = java.util.Arrays.copyOf(array, size << 1)
      array(size) = v
      size += 1
    }
    new FixedIndexedSeq[GeoRTreeEntry[A]](array, size)
  }

  /**
    * @return a sequence of all entries
    */
  def entries: IndexedSeq[GeoRTreeEntry[A]] =
    new AdaptedIndexedSeq[GeoRTree[A], GeoRTreeEntry[A]](GeoRTree.lastLevel(GeoRTree.this))

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

  private[core] def sinMinLat: Double

  private[core] def cosMinLat: Double

  private[core] def sinMaxLat: Double

  private[core] def cosMaxLat: Double
}

private final case class GeoRTreeNil[A]() extends GeoRTree[A] {
  def minLat: Float = throw new UnsupportedOperationException("GeoRTreeNil.minLat")

  def minLon: Float = throw new UnsupportedOperationException("GeoRTreeNil.minLon")

  def maxLat: Float = throw new UnsupportedOperationException("GeoRTreeNil.maxLat")

  def maxLon: Float = throw new UnsupportedOperationException("GeoRTreeNil.maxLon")

  def isEmpty: Boolean = true

  def nearest(x: Float, y: Float, sinLat: Double, cosLat: Double, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, GeoRTreeEntry[A]) => Float): Float = maxDist

  def search(x: Float, y: Float)(f: GeoRTreeEntry[A] => Unit): Unit = ()

  def search(minX: Float, minY: Float, maxX: Float, maxY: Float)(f: GeoRTreeEntry[A] => Unit): Unit = ()

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder =
    GeoRTree.appendSpaces(sb, indent).append("GeoRTreeNil()\n")

  private[core] def sinMinLat: Double = throw new UnsupportedOperationException("GeoRTreeNil.sinMinLat")

  private[core] def cosMinLat: Double = throw new UnsupportedOperationException("GeoRTreeNil.cosMinLat")

  private[core] def sinMaxLat: Double = throw new UnsupportedOperationException("GeoRTreeNil.sinMaxLat")

  private[core] def cosMaxLat: Double = throw new UnsupportedOperationException("GeoRTreeNil.cosMaxLat")
}

/**
  * Create an entry for a rectangle and a value.
  *
  * @param minLat x value of the lower left point of the given rectangle
  * @param minLon y value of the lower left point of the given rectangle
  * @param maxLat x value of the upper right point of the given rectangle
  * @param maxLon y value of the upper right point of the given rectangle
  * @param value a value to store in the r-tree
  * @tparam A a type of th value being put in the tree
  */
final case class GeoRTreeEntry[A] private[core] (minLat: Float, minLon: Float, maxLat: Float, maxLon: Float, value: A,
                                                 private[core] val sinMinLat: Double,
                                                 private[core] val cosMinLat: Double,
                                                 private[core] val sinMaxLat: Double,
                                                 private[core] val cosMaxLat: Double) extends GeoRTree[A] {
  def isEmpty: Boolean = false

  def nearest(lat: Float, lon: Float, sinLat: Double, cosLat: Double, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, GeoRTreeEntry[A]) => Float): Float = {
    val dist = GeoRTree.distance(lat, lon, sinLat, cosLat, this)
    if (dist < maxDist) f(dist, this)
    else maxDist
  }

  def search(x: Float, y: Float)(f: GeoRTreeEntry[A] => Unit): Unit =
    if (this.minLon <= y && y <= this.maxLon && this.minLat <= x && x <= this.maxLat) f(this)

  def search(minX: Float, minY: Float, maxX: Float, maxY: Float)(f: GeoRTreeEntry[A] => Unit): Unit =
    if (this.minLon <= maxY && minY <= this.maxLon && this.minLat <= maxX && minX <= this.maxLat) f(this)

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder =
    GeoRTree.appendSpaces(sb, indent).append("GeoRTreeEntry(").append(minLat).append(',').append(minLon).append(',')
      .append(maxLat).append(',').append(maxLon).append(',').append(value).append(")\n")
}

private final case class GeoRTreeNode[A](minLat: Float, minLon: Float, maxLat: Float, maxLon: Float,
                                         level: Array[GeoRTree[A]], from: Int, to: Int, sinMinLat: Double,
                                         cosMinLat: Double, sinMaxLat: Double, cosMaxLat: Double) extends GeoRTree[A] {
  def isEmpty: Boolean = false

  def nearest(lat: Float, lon: Float, sinLat: Double, cosLat: Double, maxDist: Float = Float.PositiveInfinity)
             (f: (Float, GeoRTreeEntry[A]) => Float): Float = {
    var minDist = maxDist
    val n = to - from
    var i = 0
    if (level(from).isInstanceOf[GeoRTreeEntry[A]]) {
      while (i < n) {
        val e = level(from + i).asInstanceOf[GeoRTreeEntry[A]]
        val d = GeoRTree.distance(lat, lon, sinLat, cosLat, e)
        if (d < minDist) minDist = f(d, e)
        i += 1
      }
    } else {
      val ps = new Array[Long](n)
      while (i < n) {
        ps(i) = (from + i) | (floatToRawIntBits(GeoRTree.distance(lat, lon, sinLat, cosLat, level(from + i))).toLong << 32)
        i += 1
      }
      java.util.Arrays.sort(ps) // Assuming that there no NaNs or negative values for distances
      i = 0
      while (i < n && {
        intBitsToFloat((ps(i) >> 32).toInt) < minDist && {
          minDist = level((ps(i) & 0x7fffffff).toInt).nearest(lat, lon, sinLat, cosLat, minDist)(f)
          true
        }
      }) i += 1
    }
    minDist
  }

  def search(x: Float, y: Float)(f: GeoRTreeEntry[A] => Unit): Unit =
    if (this.minLon <= y && y <= this.maxLon && this.minLat <= x && x <= this.maxLat) {
      var i = from
      while (i < to) {
        level(i).search(x, y)(f)
        i += 1
      }
    }

  def search(minX: Float, minY: Float, maxX: Float, maxY: Float)(f: GeoRTreeEntry[A] => Unit): Unit =
    if (this.minLon <= maxY && minY <= this.maxLon && this.minLat <= maxX && minX <= this.maxLat) {
      var i = from
      while (i < to) {
        level(i).search(minX, minY, maxX, maxY)(f)
        i += 1
      }
    }

  def pretty(sb: java.lang.StringBuilder, indent: Int): java.lang.StringBuilder = {
    GeoRTree.appendSpaces(sb, indent).append("GeoRTreeNode(").append(minLat).append(',').append(minLon).append(',')
      .append(maxLat).append(',').append(maxLon).append(")\n")
    var i = from
    while (i < to) {
      level(i).pretty(sb, indent + 2)
      i += 1
    }
    sb
  }

  override def equals(that: Any): Boolean = throw new UnsupportedOperationException("GeoRTreeNode.equals")

  override def hashCode(): Int = throw new UnsupportedOperationException("GeoRTreeNode.hashCode")
}
