package com.sizmek.rtree2d.core

import java.lang.Math._

object GeoUtils {
  // https://en.wikipedia.org/wiki/Haversine_formula + https://en.wikipedia.org/wiki/Earth_radius#Mean_radius
  def greatCircleDistance1(lat1: Float, lon1: Float, lat2: Float, lon2: Float, radius: Float = 6371.0088f): Float = {
    val shdy = sin((lon1 - lon2) * PI / 180 / 2)
    val shdx = sin((lat1 - lat2) * PI / 180 / 2)
    (asin(sqrt(cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * shdy * shdy + shdx * shdx)) * 2 * radius).toFloat
  }

  // https://en.wikipedia.org/wiki/Great-circle_distance + https://en.wikipedia.org/wiki/Earth_radius#Mean_radius
  def greatCircleDistance2(lat1: Float, lon1: Float, lat2: Float, lon2: Float, radius: Float = 6371.0088f): Float =
    (acos(sin(lat1 * PI / 180) * sin(lat2 * PI / 180) +
      cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * cos((lon1 - lon2) * PI / 180)) * radius).toFloat
}
