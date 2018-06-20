package com.sizmek.rtree2d.core

import java.lang.Math._

/**
  * A type class for distance calculations that can be used for `nearest` requests
  * to an R-tree instances.
  */
trait DistanceCalculator {
  /**
    * Returns a positive value for distance from the given point to a bounding box of the specified RTree,
    * or 0 if the point is with in the bounding box.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param t an RTree instance
    * @tparam A a type of th value being put in the tree
    * @return return a distance value
    */
  def distance[A](x: Float, y: Float, t: RTree[A]): Float
}

object EuclideanPlane {
  /**
    * An instance of the `DistanceCalculator` type class which use Euclidean geometry
    * to calculate distances.
    */
  implicit val distanceCalculator: DistanceCalculator = new DistanceCalculator {
    override def distance[A](x: Float, y: Float, t: RTree[A]): Float = {
      val dy = if (y < t.y1) t.y1 - y else if (y < t.y2) 0 else y - t.y2
      val dx = if (x < t.x1) t.x1 - x else if (x < t.x2) 0 else x - t.x2
      if (dy == 0) dx
      else if (dx == 0) dy
      else sqrt(dx * dx + dy * dy).toFloat
    }
  }

  /**
    * Create an entry for a rectangle and a value.
    *
    * @param x1 x coordinate of the left bottom corner
    * @param y1 y coordinate of the left bottom corner
    * @param x2 x coordinate of the right top corner
    * @param y2 y coordinate of the right top corner
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def entry[A](x1: Float, y1: Float, x2: Float, y2: Float, value: A): RTreeEntry[A] = {
    if (!(x2 >= x1)) throw new IllegalArgumentException("x2 should be greater than x1 and any of them should not be NaN")
    if (!(y2 >= y1)) throw new IllegalArgumentException("y2 should be greater than y1 and any of them should not be NaN")
    new RTreeEntry[A](x1, y1, x2, y2, value)
  }
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
}

trait Spherical {
  /**
    * Create an entry specified by a point with spherical coordinates and a value.
    *
    * @param lat a latitude coordinate of the given point
    * @param lon a latitude coordinate of the given point
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def entry[A](lat: Float, lon: Float, value: A): RTreeEntry[A] = {
    if (!(lat >= -90 && lat <= 90)) throw new IllegalArgumentException("lat should not be out of range from -90 to 90 or NaN")
    if (!(lon >= -180 && lon <= 180)) throw new IllegalArgumentException("lon should not be out of range from -180 to 180 or NaN")
    new RTreeEntry[A](lat, lon, lat, lon, value)
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
  def entry[A](minLat: Float, minLon: Float, maxLat: Float, maxLon: Float, value: A): RTreeEntry[A] = {
    if (!(minLat >= -90)) throw new IllegalArgumentException("minLat should not be less than -90 or NaN")
    if (!(minLon >= -180)) throw new IllegalArgumentException("minLon should not be less than -180 or NaN")
    if (!(maxLat <= 90 && maxLat >= minLat))
      throw new IllegalArgumentException("maxLat should not be greater than 90 or less than minLat or NaN")
    if (!(maxLon <= 180 && maxLon >= minLon))
      throw new IllegalArgumentException("maxLon should not be greater than 180 or less than minLat  or NaN")
    new RTreeEntry[A](minLat, minLon, maxLat, maxLon, value)
  }

  /**
    * Creates an instance of the `DistanceCalculator` type class which use the haversine formula to determine
    * the great-circle distance between a point and a rounding box of R-tree on a sphere with specified radius given
    * their longitudes and latitudes.
    *
    * To simplify creation of entries and queries X-axis is used for latitudes an and Y-axis for longitudes.
    *
    * Calculations was borrowed from the geoflatbush project of Vladimir Agafonkin:
    * https://github.com/mourner/geoflatbush/blob/master/index.mjs
    */
  def distanceCalculator(radius: Double): DistanceCalculator = new DistanceCalculator {
    private[this] val radPerDegree = PI / 180
    private[this] val circumference = radius * radPerDegree

    override def distance[A](lat: Float, lon: Float, t: RTree[A]): Float = {
      val minLon = t.y1
      val maxLon = t.y2
      val minLat = t.x1
      val maxLat = t.x2
      if (lon >= minLon && lon <= maxLon) {
        if (lat < minLat) ((minLat - lat) * circumference).toFloat
        else if (lat > maxLat) ((lat - maxLat) * circumference).toFloat
        else 0
      } else (acos {
        val radLat = lat * radPerDegree
        val sinLat = sin(radLat) // TODO: refactor it to be the parameter of the distance method
        val cosLat = cos(radLat) // TODO: refactor it to be the parameter of the distance method
        if (minLon == maxLon && minLat == maxLat) {
          normalizedDistanceCos(minLat, cosLat, sinLat, cos((minLon - lon) * radPerDegree))
        } else {
          val cosLonDelta = cos(min(normalize(minLon - lon), normalize(lon - maxLon)) * radPerDegree)
          val extremumLat = atan(sinLat / (cosLat * cosLonDelta)) / radPerDegree
          var d = max(
            normalizedDistanceCos(minLat, cosLat, sinLat, cosLonDelta),
            normalizedDistanceCos(maxLat, cosLat, sinLat, cosLonDelta))
          if (extremumLat > minLat && extremumLat < maxLat) {
            d = max(d, normalizedDistanceCos(extremumLat, cosLat, sinLat, cosLonDelta))
          }
          d
        }
      } * radius).toFloat
    }

    private[this] def normalize(lonDelta: Float): Float = if (lonDelta < 0) lonDelta + 360 else lonDelta

    private[this] def normalizedDistanceCos(lat: Double, cosLat: Double, sinLat: Double, cosLonDelta: Double): Double = {
      val radLat = lat * radPerDegree
      min(sinLat * sin(radLat) + cosLat * cos(radLat) * cosLonDelta, 1)
    }
  }
}

object SphericalEarth extends Spherical {
  private[this] val radPerDegree = PI / 180

  /**
    * 6371.0088 is a value of the mean radius in kilometers, see: https://en.wikipedia.org/wiki/Earth_radius#Mean_radius
    * It allows to get +0.2% accuracy on poles, -0.1% on the equator, and less than ±0.05% on medium latitudes.
    * Precision of 32-bit float number allows to locate points and calculate distances with an error ±0.5 meters.
    */
  private[this] val earthMeanRadius = 6371.0088

  /**
    * An instance of the `DistanceCalculator` type class which use a spherical model of the Earth to calculate distances
    * that are represented in kilometers.
    *
    */
  implicit val distanceCalculator: DistanceCalculator = distanceCalculator(earthMeanRadius)

  /**
    * Create an indexed sequence of entries that are specified by a circular area on the Earth and a value.
    *
    * Sequence of entries required for case when the circle is crossed by the anti-meridian because the RTree which
    * use bounding box form longitudes and latitudes for geo-indexing doesn't support wrapping of geo-coordinates over
    * the Earth, so we split that entries on two by the date change meridian.
    *
    * Used formula with description is here: http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates#Longitude
    *
    * @param lat a latitude coordinate of the given center point
    * @param lon a latitude coordinate of the given center point
    * @param distance a distance, from the center point to borders of the circular area on the Earth surface (in km),
    *                 if the value of distance is greated than half of circumference of the Earth then a whole sphere
    *                 will be bounded
    * @param value a value to store in the r-tree
    * @tparam A a type of th value being put in the tree
    * @return a newly created entry
    */
  def entries[A](lat: Float, lon: Float, distance: Float, value: A): IndexedSeq[RTreeEntry[A]] = {
    if (!(lat >= -90 && lat <= 90)) throw new IllegalArgumentException("lat should not be out of range from -90 to 90 or NaN")
    if (!(lon >= -180 && lon <= 180)) throw new IllegalArgumentException("lon should not be out of range from -180 to 180 or NaN")
    if (!(distance >= 0)) throw new IllegalArgumentException("distance should not be less than 0 or NaN")
    val radLon = lon * radPerDegree
    val radLat = lat * radPerDegree
    val deltaLat = distance / earthMeanRadius
    val lat1 = ((radLat - deltaLat) / radPerDegree).toFloat
    val lat2 = ((radLat + deltaLat) / radPerDegree).toFloat
    if (lat1 > -90 && lat2 < 90) {
      val deltaLon = asin(sin(deltaLat) / cos(radLat))
      val lon1 = ((radLon - deltaLon) / radPerDegree).toFloat
      val lon2 = ((radLon + deltaLon) / radPerDegree).toFloat
      if (lon1 < -180) {
        new IndexedSeq2(new RTreeEntry(lat1, -180, lat2, lon2, value), new RTreeEntry(lat1, lon1 + 360, lat2, 180, value))
      } else if (lon2 > 180) {
        new IndexedSeq2(new RTreeEntry(lat1, -180, lat2, lon2 - 360, value), new RTreeEntry(lat1, lon1, lat2, 180, value))
      }
      else new IndexedSeq1(new RTreeEntry(lat1, lon1, lat2, lon2, value))
    } else new IndexedSeq1(new RTreeEntry(max(lat1, -90), -180, min(lat2, 90), 180, value))
  }
}

private class IndexedSeq1[A](a: A) extends IndexedSeq[A] {
  override def length: Int = 1

  override def apply(idx: Int): A =
    if (idx == 0) a
    else throw new IndexOutOfBoundsException(idx.toString)
}

private class IndexedSeq2[A](a0: A, a1: A) extends IndexedSeq[A] {
  override def length: Int = 2

  override def apply(idx: Int): A =
    if (idx == 0) a0
    else if (idx == 1) a1
    else throw new IndexOutOfBoundsException(idx.toString)
}
