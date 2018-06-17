package com.sizmek.rtree2d.core

import java.util.concurrent.atomic.AtomicInteger

import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import org.scalatest.prop.Checkers

class RTreeCheckers extends WordSpec with Checkers {
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)
  private val lastId = new AtomicInteger
  private val floatGen = Gen.choose[Float](-1000, 1000)
  private val positiveFloatGen = Gen.choose[Float](0, 200)
  private val entryGen = for {
    x <- floatGen
    y <- floatGen
    w <- positiveFloatGen
    h <- positiveFloatGen
  } yield RTreeEntry(x, y, x + w, y + h, lastId.getAndIncrement())
  private val entryListGen = Gen.oneOf(0, 1, 10, 100, 1000).flatMap(n => Gen.listOfN(n, entryGen))

  "RTree" when {
    "update" should {
      "withdraw matched entries from a rtree" in check {
        forAll(entryListGen, entryListGen) {
          (entries1: List[RTreeEntry[Int]], entries2: List[RTreeEntry[Int]]) =>
            val entries12 = entries1 ++ entries2
            val expected = entries1.sorted
            RTree.update(RTree(entries12), entries2, Nil).entries.sorted ?= expected
        }
      }
      "build new rtree with old and inserted entries" in check {
        forAll(entryListGen, entryListGen) {
          (entries1: List[RTreeEntry[Int]], entries3: List[RTreeEntry[Int]]) =>
            val expected = (entries1 ++ entries3).sorted
            RTree.update(RTree(entries1), Nil, entries3).entries.sorted ?= expected
        }
      }
      "remove and insert at the same time properly" in check {
        forAll(entryListGen, entryListGen, entryListGen) {
          (entries1: List[RTreeEntry[Int]], entries2: List[RTreeEntry[Int]], entries3: List[RTreeEntry[Int]]) =>
            val entries12 = entries1 ++ entries2
            val expected = (entries1 ++ entries3).sorted
            RTree.update(RTree(entries12), entries2, entries3).entries.sorted ?= expected
        }
      }
    }
    "asked for entries" should {
      "return all entries" in check {
        forAll(entryListGen) {
          (entries: List[RTreeEntry[Int]]) =>
            val expected = entries.sorted
            RTree(entries).entries.sorted ?= expected
        }
      }
    }
    "asked for nearest" should {
      "return any of entries which intersects by point" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: List[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanDistanceCalculator._
            val sorted = entries.map(e => (calculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && sorted.exists { case (d, e) => d == 0.0f }) ==> {
              val result = RTree(entries).nearest(x, y)
              sorted.map(Some(_)).contains(result)
            }
        }
      }
      "return the nearest entry if point is out of all entries" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: List[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanDistanceCalculator._
            val sorted = entries.map(e => (calculator.distance(x, y, e), e)).sortBy(_._1)
            propBoolean(sorted.nonEmpty && !sorted.exists { case (d, e) => d == 0.0f }) ==> {
              RTree(entries).nearest(x, y) ?= sorted.headOption
            }
        }
      }
      "don't return any entry for empty tree" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: List[RTreeEntry[Int]], x: Float, y: Float) =>
            import EuclideanDistanceCalculator._
            propBoolean(entries.isEmpty) ==> {
              RTree(entries).nearest(x, y) ?= None
            }
        }
      }
    }
    "full searched by point" should {
      "receive value of all matched entries" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: List[RTreeEntry[Int]], x: Float, y: Float) =>
            val expected = intersects(entries, x, y).sorted
            propBoolean(expected.nonEmpty) ==> {
              RTree(entries).searchAll(x, y).sorted ?= expected
            }
        }
      }
      "don't receive any value if no matches" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: List[RTreeEntry[Int]], x: Float, y: Float) =>
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
          (entries: List[RTreeEntry[Int]], x: Float, y: Float) =>
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
          (entries: List[RTreeEntry[Int]], x: Float, y: Float, w: Float, h: Float) =>
            val (x1, y1, x2, y2) = (x, y, x + w, y + h)
            val expected = intersects(entries, x1, y1, x2, y2).sorted
            propBoolean(expected.nonEmpty) ==> {
              RTree(entries).searchAll(x1, y1, x2, y2).sorted ?= expected
            }
        }
      }
      "don't receive any value if no matches" in check {
        forAll(entryListGen, floatGen, floatGen, positiveFloatGen, positiveFloatGen) {
          (entries: List[RTreeEntry[Int]], x: Float, y: Float, w: Float, h: Float) =>
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
          (entries: List[RTreeEntry[Int]], x: Float, y: Float, w: Float, h: Float) =>
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
  "EuclideanDistanceCalculator.calculator" when {
    "asked to calculate distance from point to an RTree" should {
      "return a distance to the RTree bounding box in case point is out of it" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: List[RTreeEntry[Int]], x: Float, y: Float) =>
            val t = RTree(entries)
            propBoolean(entries.nonEmpty && !intersects(t, x, y)) ==> {
              val dx = Math.max(Math.abs((t.x1 + t.x2) / 2 - x) - (t.x2 - t.x1) / 2, 0)
              val dy = Math.max(Math.abs((t.y1 + t.y2) / 2 - y) - (t.y2 - t.y1) / 2, 0)
              val expected = Math.sqrt(dx * dx + dy * dy).toFloat
              EuclideanDistanceCalculator.calculator.distance(x, y, t) === expected +- 0.001f
            }
        }
      }
      "return 0 in case point is intersects with RTree bounding box" in check {
        forAll(entryListGen, floatGen, floatGen) {
          (entries: List[RTreeEntry[Int]], x: Float, y: Float) =>
            val t = RTree(entries)
            propBoolean(entries.nonEmpty && intersects(t, x, y)) ==> {
              EuclideanDistanceCalculator.calculator.distance(x, y, t) ?= 0
            }
        }
      }
    }
  }

  private def intersects[T](es: Seq[RTreeEntry[T]], x: Float, y: Float): Seq[RTreeEntry[T]] =
    intersects(es, x, y, x, y)

  private def intersects[T](es: Seq[RTreeEntry[T]], x1: Float, y1: Float, x2: Float, y2: Float): Seq[RTreeEntry[T]] =
    es.filter(e => intersects(e, x1, y1, x2, y2))

  private def intersects[T](e: RTree[T], x: Float, y: Float): Boolean =
    e.x1 <= x && x <= e.x2 && e.y1 <= y && y <= e.y2

  private def intersects[T](e: RTree[T], x1: Float, y1: Float, x2: Float, y2: Float): Boolean =
    e.x1 <= x2 && x1 <= e.x2 && e.y1 <= y2 && y1 <= e.y2

  implicit private def orderingByName[A <: RTreeEntry[Int]]: Ordering[A] =
    Ordering.by(e => (e.x1, e.y1, e.x2, e.y2, e.value))
}
