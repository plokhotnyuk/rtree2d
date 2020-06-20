package com.github.plokhotnyuk.rtree2d.benchmark

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import collection.JavaConverters._

class DavidMotenRTreeTest extends AnyWordSpec with Matchers {
  def benchmark(shuffling: Boolean): DavidMotenRTree = new DavidMotenRTree {
    shuffle = shuffling
    setup()
  }

  def testWith(benchmark: () => DavidMotenRTree): Unit = {
    benchmark().apply.entries().asScala.toSet shouldBe benchmark().rtree.entries().asScala.toSet
    benchmark().entries should contain allElementsOf benchmark().rtreeEntries
    benchmark().update.entries().asScala.toList should contain allElementsOf benchmark().rtreeEntries.diff(benchmark().entriesToRemove) ++ benchmark().entriesToAdd
    benchmark().nearest should contain oneElementOf benchmark().rtreeEntries
    val b = benchmark()
    (1 to b.size * 2).foreach(_ => b.nearestK.size shouldBe b.nearestMax)
    (1 to b.size * 2).foreach(_ => b.searchByPoint.size shouldBe 1)
    (1 to b.size * 2).foreach(_ => b.searchByRectangle.size should be >= 1)
  }

  "DavidMotenRTree" should {
    "return proper values without shuffling" in testWith(() => benchmark(shuffling = false))
    "return proper values with shuffling" in testWith(() => benchmark(shuffling = true))
  }
}
