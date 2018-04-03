package com.sizmek.rtree2d.core

import java.util.concurrent.atomic.AtomicInteger

import org.scalacheck.Prop._
import org.scalacheck.Gen
import org.scalatest.WordSpec
import org.scalatest.prop.Checkers

class RTreeCheckers extends WordSpec with Checkers {
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)
  private val lastId = new AtomicInteger
  private val floatGen = Gen.choose[Float](-1000, 1000)
  private val positiveFloatGen = Gen.choose[Float](0, 333)
  private val entryGen = for {
    x <- floatGen
    y <- floatGen
    w <- positiveFloatGen
    h <- positiveFloatGen
  } yield RTreeEntry(x, y, x + w, y + h, lastId.getAndIncrement())
  private val entryListGen = Gen.oneOf(0, 1, 10, 100, 1000, 10000).flatMap(n => Gen.listOfN(n, entryGen))

  "RTree" when {
    "asked for entries" should {
      "return all entries" in check {
        forAll(entryListGen) {
          (entries: List[RTreeEntry[Int]]) =>
            val expected = entries.sorted
            RTree(entries).entries.sorted ?= expected
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

  private def intersects[T](es: Seq[RTreeEntry[T]], x: Float, y: Float): Seq[RTreeEntry[T]] =
    intersects(es, x, y, x, y)

  private def intersects[T](es: Seq[RTreeEntry[T]], x1: Float, y1: Float, x2: Float, y2: Float): Seq[RTreeEntry[T]] =
    es.filter(e => intersects(e, x1, y1, x2, y2))

  private def intersects[T](e: RTreeEntry[T], x1: Float, y1: Float, x2: Float, y2: Float): Boolean =
    e.x1 <= x2 && x1 <= e.x2 && e.y1 <= y2 && y1 <= e.y2

  implicit private def orderingByName[A <: RTreeEntry[Int]]: Ordering[A] =
    Ordering.by(e => (e.x1, e.y1, e.x2, e.y2, e.value))
}
