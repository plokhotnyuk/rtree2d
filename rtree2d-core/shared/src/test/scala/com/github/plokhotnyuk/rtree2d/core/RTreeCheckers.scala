package com.github.plokhotnyuk.rtree2d.core

import TestUtils._
import org.scalacheck.Prop._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class RTreeCheckers extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 1000)
  "RTree" when {
    "update" should {
      "withdraw matched entries from a rtree" in {
        forAll(entryListGen, entryListGen) {
          (entries1: Seq[RTreeEntry[Int]], entries2: Seq[RTreeEntry[Int]]) =>
            val entries12 = entries1 ++ entries2
            val expected = entries1.sorted
            RTree.update(RTree(entries12), entries2, Nil).entries.sorted === expected
        }
      }
      "build new rtree with old and inserted entries" in {
        forAll(entryListGen, entryListGen) {
          (entries1: Seq[RTreeEntry[Int]], entries3: Seq[RTreeEntry[Int]]) =>
            val expected = (entries1 ++ entries3).sorted
            RTree.update(RTree(entries1), Nil, entries3).entries.sorted === expected
        }
      }
      "remove and insert at the same time properly" in {
        forAll(entryListGen, entryListGen, entryListGen) {
          (entries1: Seq[RTreeEntry[Int]], entries2: Seq[RTreeEntry[Int]], entries3: Seq[RTreeEntry[Int]]) =>
            val entries12 = entries1 ++ entries2
            val expected = (entries1 ++ entries3).sorted
            RTree.update(RTree(entries12), entries2, entries3).entries.sorted === expected
        }
      }
    }
    "asked for entries" should {
      "return all entries" in {
        forAll(entryListGen) {
          (entries: Seq[RTreeEntry[Int]]) =>
            val expected = entries.sorted
            RTree(entries).entries.sorted === expected
        }
      }
    }
    "asked for nearest one" should {
      "return any of entries which intersects with point" in {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && sorted.exists(_._1 == 0.0f)) ==> {
              entries.contains(RTree(entries).nearestOption(x, y).get)
            }
        }
      }
      "return the nearest entry if point does not intersect with entries" in {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && !sorted.exists(_._1 == 0.0f)) ==> {
              RTree(entries).nearestOption(x, y) === Some(sorted.head._2)
            }
        }
      }
      "return the nearest entry with in a specified distance limit or none if all entries are out of the limit" in {
        forAll(entryListGen, floatGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, maxDist: Float) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).filter(_._1 < maxDist).sortBy(_._1)
            propBoolean(sorted.nonEmpty) ==> {
              val result = RTree(entries).nearestOption(x, y, maxDist).get
              entries.contains(result)
            }
        }
      }
      "don't return any entry for empty tree" in {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanPlane._
            propBoolean(entries.isEmpty) ==> {
              RTree(entries).nearestOption(x, y) === None
            }
        }
      }
    }
    "asked for nearest K" should {
      "return up to K entries which intersects with point" in {
        forAll(entryListGen, floatGen, floatGen, positiveIntGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, k: Int) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && sorted.exists(_._1 == 0.0f)) ==> {
              val result = RTree(entries).nearestK(x, y, k)
              result.forall(entries.contains)
            }
        }
      }
      "return up to K nearest entries if point does not intersect with entries" in {
        forAll(entryListGen, floatGen, floatGen, positiveIntGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, k: Int) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && !sorted.exists(_._1 == 0.0f) && sorted.size == sorted.map(_._1).distinct.size) ==> {
              RTree(entries).nearestK(x, y, k).toSet === sorted.take(k).map(_._2).toSet
            }
        }
      }
      "return up to K nearest entries with in a specified distance limit or none if all entries are out of the limit" in {
        forAll(entryListGen, floatGen, floatGen, floatGen, positiveIntGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, maxDist: Float, k: Int) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).filter(_._1 < maxDist).sortBy(_._1)
            propBoolean(sorted.size == sorted.map(_._1).distinct.size) ==> {
              RTree(entries).nearestK(x, y, k, maxDist).toSet === sorted.take(k).map(_._2).toSet
            }
        }
      }
      "don't return any entry for empty tree" in {
        forAll(entryListGen, floatGen, floatGen, positiveIntGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, k: Int) =>
            import EuclideanPlane._
            propBoolean(entries.isEmpty) ==> {
              RTree(entries).nearestK(x, y, k) === Seq()
            }
        }
      }
    }
    "full searched by point" should {
      "receive value of all matched entries" in {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            val expected = intersects(entries, x, y).sorted
            propBoolean(expected.nonEmpty) ==> {
              RTree(entries).searchAll(x, y).sorted === expected
            }
        }
      }
      "don't receive any value if no matches" in {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            val expected = intersects(entries, x, y)
            propBoolean(expected.isEmpty) ==> {
              RTree(entries).searchAll(x, y).isEmpty
            }
        }
      }
    }
    "full searched by rectangle" should {
      "receive value of all matched entries" in {
        forAll(entryListGen, floatGen, floatGen, positiveFloatGen, positiveFloatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, w: Float, h: Float) =>
            val (minX, minY, maxX, maxY) = (x, y, x + w, y + h)
            val expected = intersects(entries, minX, minY, maxX, maxY).sorted
            propBoolean(expected.nonEmpty) ==> {
              RTree(entries).searchAll(minX, minY, maxX, maxY).sorted === expected
            }
        }
      }
      "don't receive any value if no matches" in {
        forAll(entryListGen, floatGen, floatGen, positiveFloatGen, positiveFloatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, w: Float, h: Float) =>
            val (minX, minY, maxX, maxY) = (x, y, x + w, y + h)
            val expected = intersects(entries, minX, minY, maxX, maxY)
            propBoolean(expected.isEmpty) ==> {
              RTree(entries).searchAll(minX, minY, maxX, maxY).isEmpty
            }
        }
      }
    }
  }
  "RTreeEntryBinaryHeap" when {
    "number of added distance/entry pairs not greater than the max size" should {
      "return max distance provided in constructor" in {
        forAll(distanceEntryListGen, positiveFloatGen, positiveIntGen) {
          (distanceEntryPairs: Seq[(Float, RTreeEntry[Int])], maxDist: Float, maxSize: Int) =>
            propBoolean(distanceEntryPairs.size < maxSize && distanceEntryPairs.forall(_._1 < maxDist)) ==> {
              val heap = new RTreeEntryBinaryHeap[Int](maxDist, maxSize)
              distanceEntryPairs.forall { case (d, e) => heap.put(d, e) == maxDist }
            }
        }
      }
      "return sequence with all submitted distance/entry pairs" in {
        forAll(distanceEntryListGen, positiveFloatGen, positiveIntGen) {
          (distanceEntryPairs: Seq[(Float, RTreeEntry[Int])], maxDist: Float, maxSize: Int) =>
            propBoolean(distanceEntryPairs.size <= maxSize) ==> {
              val heap = new RTreeEntryBinaryHeap[Int](maxDist, maxSize)
              distanceEntryPairs.foreach { case (d, e) => heap.put(d, e) }
              heap.toIndexedSeq.toSet === distanceEntryPairs.map(_._2).toSet
            }
        }
      }
    }
    "number of added distance/entry pairs greater than the max size" should {
      "return sequence with most nearest distance/entry pairs from all submitted" in {
        forAll(distanceEntryListGen, positiveIntGen) {
          (distanceEntryPairs: Seq[(Float, RTreeEntry[Int])], maxSize: Int) =>
            val size = distanceEntryPairs.size
            propBoolean(size > maxSize && size == distanceEntryPairs.map(_._1).distinct.size) ==> {
              val heap = new RTreeEntryBinaryHeap[Int](Float.PositiveInfinity, maxSize)
              distanceEntryPairs.foreach { case (d, e) => heap.put(d, e) }
              heap.toIndexedSeq.toSet === distanceEntryPairs.sortBy(_._1).take(maxSize).map(_._2).toSet
            }
        }
      }
    }
  }
}
