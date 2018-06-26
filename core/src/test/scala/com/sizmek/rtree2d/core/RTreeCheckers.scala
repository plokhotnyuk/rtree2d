package com.sizmek.rtree2d.core

import com.sizmek.rtree2d.core.TestUtils._
import org.scalacheck.Prop._
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.prop.Checkers

class RTreeCheckers extends WordSpec with Checkers {
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)
  "RTree" when {
    "update" should {
      "withdraw matched entries from a rtree" in check {
        forAll(entryListGen, entryListGen) {
          (entries1: Seq[RTreeEntry[Int]], entries2: Seq[RTreeEntry[Int]]) =>
            val entries12 = entries1 ++ entries2
            val expected = entries1.sorted
            RTree.update(RTree(entries12), entries2, Nil).entries.sorted === expected
        }
      }
      "build new rtree with old and inserted entries" in check {
        forAll(entryListGen, entryListGen) {
          (entries1: Seq[RTreeEntry[Int]], entries3: Seq[RTreeEntry[Int]]) =>
            val expected = (entries1 ++ entries3).sorted
            RTree.update(RTree(entries1), Nil, entries3).entries.sorted === expected
        }
      }
      "remove and insert at the same time properly" in check {
        forAll(entryListGen, entryListGen, entryListGen) {
          (entries1: Seq[RTreeEntry[Int]], entries2: Seq[RTreeEntry[Int]], entries3: Seq[RTreeEntry[Int]]) =>
            val entries12 = entries1 ++ entries2
            val expected = (entries1 ++ entries3).sorted
            RTree.update(RTree(entries12), entries2, entries3).entries.sorted === expected
        }
      }
    }
    "asked for entries" should {
      "return all entries" in check {
        forAll(entryListGen) {
          (entries: Seq[RTreeEntry[Int]]) =>
            val expected = entries.sorted
            RTree(entries).entries.sorted === expected
        }
      }
    }
    "asked for nearest one" should {
      "return any of entries which intersects with point" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && sorted.exists { case (d, e) => d == 0.0f }) ==> {
              val result = RTree(entries).nearestOption(x, y).get
              sorted.map(_._2).contains(result)
            }
        }
      }
      "return the nearest entry if point does not intersect with entries" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && !sorted.exists { case (d, e) => d == 0.0f }) ==> {
              RTree(entries).nearestOption(x, y) === Some(sorted.head._2)
            }
        }
      }
      "return the nearest entry with in a specified distance limit or none if all entries are out of the limit" in check {
        forAll(entryListGen, floatGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, maxDist: Float) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).filter(_._1 < maxDist).sortBy(_._1)
            propBoolean(sorted.nonEmpty) ==> {
              val result = RTree(entries).nearestOption(x, y, maxDist)
              sorted.map { case (d, e) =>Some(e) }.contains(result)
            }
        }
      }
      "don't return any entry for empty tree" in check {
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
      "return up to K entries which intersects with point" in check {
        forAll(entryListGen, floatGen, floatGen, positiveIntGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, k: Int) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && sorted.exists { case (d, e) => d == 0.0f }) ==> {
              val result = RTree(entries).nearestK(x, y, k)
              result.forall(sorted.map(_._2).contains)
            }
        }
      }
      "return up to K nearest entries if point does not intersect with entries" in check {
        forAll(entryListGen, floatGen, floatGen, positiveIntGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, k: Int) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && !sorted.exists { case (d, e) => d == 0.0f } && sorted.size == sorted.map(_._1).distinct.size) ==> {
              RTree(entries).nearestK(x, y, k).toSet === sorted.take(k).map(_._2).toSet
            }
        }
      }
      "return up to K nearest entries with in a specified distance limit or none if all entries are out of the limit" in check {
        forAll(entryListGen, floatGen, floatGen, floatGen, positiveIntGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, maxDist: Float, k: Int) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).filter(_._1 < maxDist).sortBy(_._1)
            propBoolean(sorted.size == sorted.map(_._1).distinct.size) ==> {
              RTree(entries).nearestK(x, y, k, maxDist).toSet === sorted.take(k).map(_._2).toSet
            }
        }
      }
      "don't return any entry for empty tree" in check {
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
      "receive value of all matched entries" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            val expected = intersects(entries, x, y).sorted
            propBoolean(expected.nonEmpty) ==> {
              RTree(entries).searchAll(x, y).sorted === expected
            }
        }
      }
      "don't receive any value if no matches" in check {
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
      "receive value of all matched entries" in check {
        forAll(entryListGen, floatGen, floatGen, positiveFloatGen, positiveFloatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, w: Float, h: Float) =>
            val (minX, minY, maxX, maxY) = (x, y, x + w, y + h)
            val expected = intersects(entries, minX, minY, maxX, maxY).sorted
            propBoolean(expected.nonEmpty) ==> {
              RTree(entries).searchAll(minX, minY, maxX, maxY).sorted === expected
            }
        }
      }
      "don't receive any value if no matches" in check {
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
}
