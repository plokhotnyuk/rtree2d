package com.github.plokhotnyuk.rtree2d.core

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
    * @tparam A a type of the value being put in the r-tree
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
      val dy = if (y < t.minY) t.minY - y else if (y < t.maxY) 0 else y - t.maxY
      val dx = if (x < t.minX) t.minX - x else if (x < t.maxX) 0 else x - t.maxX
      if (dy == 0) dx
      else if (dx == 0) dy
      else sqrt(dx * dx + dy * dy).toFloat
    }
  }

  /**
    * Create an entry for a rectangle and a value.
    *
    * @param minX x coordinate of the left bottom corner
    * @param minY y coordinate of the left bottom corner
    * @param maxX x coordinate of the right top corner
    * @param maxY y coordinate of the right top corner
    * @param value a value to store in the r-tree
    * @tparam A a type of the value being put in the r-tree
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
    * Create an entry for a point and a value.
    *
    * @param x x value of the given point
    * @param y y value of the given point
    * @param value a value to store in the r-tree
    * @tparam A a type of the value being put in the r-tree
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
    * @tparam A a type of the value being put in the r-tree
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
  private[this] val radPerDegree = PI / 180
  private[this] val degreePerRad = 180 / PI

  /**
    * Create an entry specified by a point with spherical coordinates and a value.
    *
    * @param lat a latitude coordinate of the given point
    * @param lon a latitude coordinate of the given point
    * @param value a value to store in the r-tree
    * @tparam A a type of the value being put in the r-tree
    * @return a newly created entry
    */
  def entry[A](lat: Float, lon: Float, value: A): RTreeEntry[A] = {
    if (!(lat >= -90 && lat <= 90))
      throw new IllegalArgumentException("lat should not be out of range from -90 to 90 or NaN")
    if (!(lon >= -180 && lon <= 180))
      throw new IllegalArgumentException("lon should not be out of range from -180 to 180 or NaN")
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
    * @tparam A a type of the value being put in the r-tree
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
    * Create an indexed sequence of entries that are specified by a circular area on the sphere and a value.
    *
    * Sequence of entries required for case when the circle is crossed by the anti-meridian because the RTree which
    * use bounding box form longitudes and latitudes for indexing doesn't support wrapping of longitudes over
    * the sphere, so we split that entries on two by the date change meridian.
    *
    * Used formula with description is here: http://janmatuschek.de/LatitudeLongitudeBoundingCoordinates#Longitude
    *
    * @param lat a latitude coordinate of the given center point
    * @param lon a latitude coordinate of the given center point
    * @param distance a distance, from the center point to borders of the circular area on the sphere surface,
    *                 if the value of distance is greater than a half of the circumference then a whole sphere
    *                 will be bounded
    * @param radius a value of radius to calculate
    * @param value a value to store in the r-tree
    * @tparam A a type of the value being put in the r-tree
    * @return a newly created entry
    */
  def entries[A](lat: Float, lon: Float, distance: Float, value: A, radius: Double): IndexedSeq[RTreeEntry[A]] = {
    if (!(lat >= -90 && lat <= 90))
      throw new IllegalArgumentException("lat should not be out of range from -90 to 90 or NaN")
    if (!(lon >= -180 && lon <= 180))
      throw new IllegalArgumentException("lon should not be out of range from -180 to 180 or NaN")
    if (!(distance >= 0)) throw new IllegalArgumentException("distance should not be less than 0 or NaN")
    val radPerDegree = this.radPerDegree
    val radLon = lon * radPerDegree
    val radLat = lat * radPerDegree
    val latDelta = distance / radius
    val degreePerRad = this.degreePerRad
    val minLat = ((radLat - latDelta) * degreePerRad).toFloat
    val maxLat = ((radLat + latDelta) * degreePerRad).toFloat
    if (minLat > -90 && maxLat < 90) {
      val lonDelta = asin(sin(latDelta) / cos(radLat))
      val minLon = ((radLon - lonDelta) * degreePerRad).toFloat
      val maxLon = ((radLon + lonDelta) * degreePerRad).toFloat
      if (minLon < -180) {
        new IndexedSeq2(new RTreeEntry(minLat, -180, maxLat, maxLon, value),
          new RTreeEntry(minLat, minLon + 360, maxLat, 180, value))
      } else if (maxLon > 180) {
        new IndexedSeq2(new RTreeEntry(minLat, -180, maxLat, maxLon - 360, value),
          new RTreeEntry(minLat, minLon, maxLat, 180, value))
      } else new IndexedSeq1(new RTreeEntry(minLat, minLon, maxLat, maxLon, value))
    } else new IndexedSeq1(new RTreeEntry(max(minLat, -90), -180, min(maxLat, 90), 180, value))
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
    private[this] val circumference = radius * radPerDegree

    override def distance[A](lat: Float, lon: Float, t: RTree[A]): Float = {
      val minLon = t.minY
      val maxLon = t.maxY
      val minLat = t.minX
      val maxLat = t.maxX
      if (lon >= minLon && lon <= maxLon) {
        if (lat < minLat) (minLat - lat) * circumference
        else if (lat > maxLat) (lat - maxLat) * circumference
        else 0
      } else acos(min({
        val radPerDegree = Spherical.this.radPerDegree
        val latSin = sin(lat * radPerDegree)
        val latCos = sqrt(1 - latSin * latSin)
        val minLatSin = sin(minLat * radPerDegree)
        val minLatCos = sqrt(1 - minLatSin * minLatSin)
        if (minLon == maxLon && minLat == maxLat) {
          latSin * minLatSin + latCos * cos((minLon - lon) * radPerDegree) * minLatCos
        } else {
          val maxLatSin = sin(maxLat * radPerDegree)
          val maxLatCos = sqrt(1 - maxLatSin * maxLatSin)
          val latCosPerLonDeltaCos = latCos * cos(min(normalize(minLon - lon), normalize(lon - maxLon)) * radPerDegree)
          val normalizedDistanceCos = max(
            latSin * minLatSin + latCosPerLonDeltaCos * minLatCos,
            latSin * maxLatSin + latCosPerLonDeltaCos * maxLatCos)
          val extremumLatTan = latSin / latCosPerLonDeltaCos
          if (extremumLatTan * minLatCos <= minLatSin || extremumLatTan * maxLatCos >= maxLatSin) normalizedDistanceCos
          else {
            val extremumLatCos = sqrt(1 / (1 + extremumLatTan * extremumLatTan))
            val extremumLatSin = extremumLatTan / extremumLatCos
            max(latSin * extremumLatSin + latCosPerLonDeltaCos * extremumLatCos, normalizedDistanceCos)
          }
        }
      }, 1)) * radius
    }.toFloat

    private[this] def normalize(lonDelta: Float): Float = if (lonDelta < 0) lonDelta + 360 else lonDelta
  }
}

object SphericalEarth extends Spherical {
  private[this] val earthMeanRadius = 6371.0088

  /**
    * An instance of the `DistanceCalculator` type class which use a spherical model of the Earth to calculate distances
    * that are represented in kilometers.
    *
    * It is use 6371.0088 as a value of the mean radius in kilometers, see: https://en.wikipedia.org/wiki/Earth_radius#Mean_radius
    * It allows to get +0.2% accuracy on poles, -0.1% on the equator, and less than ±0.05% on medium latitudes.
    *
    * If your indexed entries and requests are located in some area that is less than half-sphere than you can use
    * mean radius between min and max radiuses in used range of latitudes to get much better accuracy.
    *
    * Use the `EllipsoidalEarth.radius` function to calculate it for different latitudes.
    *
    * Precision of 32-bit float number allows to locate points and calculate distances with an error ±0.5 meters.
    */
  implicit val distanceCalculator: DistanceCalculator = distanceCalculator(earthMeanRadius)

  /**
    * Create an indexed sequence of entries that are specified by a circular area on the Earth and a value.
    *
    * Sequence of entries required for case when the circle is crossed by the anti-meridian because the RTree which
    * use bounding box form longitudes and latitudes for indexing doesn't support wrapping of longitudes over
    * the Earth, so we split that entries on two by the date change meridian.
    *
    * @param lat a latitude coordinate of the given center point
    * @param lon a latitude coordinate of the given center point
    * @param distance a distance, from the center point to borders of the circular area on the Earth surface,
    *                 if the value of distance is greater than a half of the circumference then the whole Earth
    *                 will be bounded
    * @param value a value to store in the r-tree
    * @tparam A a type of the value being put in the r-tree
    * @return a newly created entry
    */
  def entries[A](lat: Float, lon: Float, distance: Float, value: A): IndexedSeq[RTreeEntry[A]] =
    entries(lat, lon, distance, value, earthMeanRadius)
}

object EllipsoidalEarth {
  private[this] val radPerDegree = PI / 180
  private[this] val earthEquatorialRadius = 6378.1370
  private[this] val earthPolarRadius = 6356.7523

  /**
    * Calculate a distance to the center of ellipsoidal model of the Earth (in km) at the specified latitude.
    *
    * @param lat a value of provided latitude in degrees
    * @return a radius (in km)
    */
  def radius(lat: Float): Double = {
    val latSin = sin(lat * radPerDegree)
    val latCos = sqrt(1 - latSin * latSin)
    val s1 = earthEquatorialRadius * latCos
    val s2 = earthPolarRadius * latSin
    val s3 = earthEquatorialRadius * s1
    val s4 = earthPolarRadius * s2
    sqrt((s3 * s3 + s4 * s4) / (s1 * s1 + s2 * s2))
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
