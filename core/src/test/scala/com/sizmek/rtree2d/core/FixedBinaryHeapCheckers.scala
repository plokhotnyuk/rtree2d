package com.sizmek.rtree2d.core

import com.sizmek.rtree2d.core.TestUtils._
import org.scalacheck.Prop._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.prop.Checkers

class FixedBinaryHeapCheckers extends WordSpec with Checkers {
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)
  "FixedBinaryHeap" when {
    "number of added distance/entry pairs not greater than the max size" should {
      "return max distance provided in constructor" in check {
        forAll(distanceEntryListGen, positiveFloatGen, positiveIntGen) {
          (distanceEntryPairs: Seq[(Float, RTreeEntry[Int])], maxDist: Float, maxSize: Int) =>
            propBoolean(distanceEntryPairs.size < maxSize && distanceEntryPairs.forall { case (d, e) => d < maxDist }) ==> {
              val heap = new FixedBinaryHeap[RTreeEntry[Int]](maxDist, maxSize)
              distanceEntryPairs.forall { case (d, e) => heap.put(d, e) == maxDist }
            }
        }
      }
      "return sequence with all submitted distance/entry pairs" in check {
        forAll(distanceEntryListGen, positiveFloatGen, positiveIntGen) {
          (distanceEntryPairs: Seq[(Float, RTreeEntry[Int])], maxDist: Float, maxSize: Int) =>
            propBoolean(distanceEntryPairs.size <= maxSize) ==> {
              val heap = new FixedBinaryHeap[RTreeEntry[Int]](maxDist, maxSize)
              distanceEntryPairs.foreach { case (d, e) => heap.put(d, e) }
              heap.toIndexedSeq.toSet === distanceEntryPairs.map(_._2).toSet
            }
        }
      }
    }
    "number of added distance/entry pairs greater than the max size" should {
      "return sequence with most nearest distance/entry pairs from all submitted" in check {
        forAll(distanceEntryListGen, positiveIntGen) {
          (distanceEntryPairs: Seq[(Float, RTreeEntry[Int])], maxSize: Int) =>
            val size = distanceEntryPairs.size
            propBoolean(size > maxSize && size == distanceEntryPairs.map(_._1).distinct.size) ==> {
              val heap = new FixedBinaryHeap[RTreeEntry[Int]](Float.PositiveInfinity, maxSize)
              distanceEntryPairs.foreach { case (d, e) => heap.put(d, e) }
              heap.toIndexedSeq.toSet === distanceEntryPairs.sortBy(_._1).take(maxSize).map(_._2).toSet
            }
        }
      }
    }
  }
}
