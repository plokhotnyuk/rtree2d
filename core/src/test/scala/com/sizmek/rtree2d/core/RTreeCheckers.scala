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
    "asked for nearest" should {
      "return any of entries which intersects by point" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && sorted.exists { case (d, e) => d == 0.0f }) ==> {
              val result = RTree(entries).nearest(x, y)
              sorted.map(Some(_)).contains(result)
            }
        }
      }
      "return the nearest entry if point is out of all entries" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && !sorted.exists { case (d, e) => d == 0.0f }) ==> {
              RTree(entries).nearest(x, y) === Some(sorted.head)
            }
        }
      }
      "return the nearest entry with in a specified distance limit or none if all entries are out of the limit" in check {
        forAll(entryListGen, floatGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, maxDist: Float) =>
            import EuclideanPlane._
            val sorted = entries.map(e => (distanceCalculator.distance(x, y, e), e)).filter(_._1 < maxDist).sortBy(_._1)
            propBoolean(sorted.nonEmpty) ==> {
              val result = RTree(entries).nearest(x, y, maxDist)
              sorted.map(Some(_)).contains(result)
            }
        }
      }
      "don't return any entry for empty tree" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanPlane._
            propBoolean(entries.isEmpty) ==> {
              RTree(entries).nearest(x, y) === None
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
    "searched by point for first match only" should {
      "receive one entry from expected" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float) =>
            val expected = intersects(entries, x, y).toSet
            propBoolean(expected.nonEmpty) ==> {
              var found: RTreeEntry[Int] = null
              RTree(entries).search(x, y) { e =>
                found = e
                true
              }
              expected(found)
            }
        }
      }
    }
    "full searched by rectangle" should {
      "receive value of all matched entries" in check {
        forAll(entryListGen, floatGen, floatGen, positiveFloatGen, positiveFloatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, w: Float, h: Float) =>
            val (x1, y1, x2, y2) = (x, y, x + w, y + h)
            val expected = intersects(entries, x1, y1, x2, y2).sorted
            propBoolean(expected.nonEmpty) ==> {
              RTree(entries).searchAll(x1, y1, x2, y2).sorted === expected
            }
        }
      }
      "don't receive any value if no matches" in check {
        forAll(entryListGen, floatGen, floatGen, positiveFloatGen, positiveFloatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, w: Float, h: Float) =>
            val (x1, y1, x2, y2) = (x, y, x + w, y + h)
            val expected = intersects(entries, x1, y1, x2, y2)
            propBoolean(expected.isEmpty) ==> {
              RTree(entries).searchAll(x1, y1, x2, y2).isEmpty
            }
        }
      }
    }
    "searched by rectangle for first match only" should {
      "receive one entry from expected" in check {
        forAll(entryListGen, floatGen, floatGen, positiveFloatGen, positiveFloatGen) {
          (entries: Seq[RTreeEntry[Int]], x: Float, y: Float, w: Float, h: Float) =>
            val (x1, y1, x2, y2) = (x, y, x + w, y + h)
            val expected = intersects(entries, x1, y1, x2, y2).toSet
            propBoolean(expected.nonEmpty) ==> {
              var found: RTreeEntry[Int] = null
              RTree(entries).search(x1, y1, x2, y2) { e =>
                found = e
                true
              }
              expected(found)
            }
        }
      }
    }
  }
}
