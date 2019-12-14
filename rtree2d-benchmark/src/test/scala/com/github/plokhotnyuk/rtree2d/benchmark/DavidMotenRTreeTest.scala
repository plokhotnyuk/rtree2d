package com.github.plokhotnyuk.rtree2d.benchmark

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import collection.JavaConverters._

class DavidMotenRTreeTest extends AnyWordSpec with Matchers {
  private def benchmark: DavidMotenRTree = new DavidMotenRTree {
    setup()
  }
  "DavidMotenRTree" should {
    "return proper values" in {
      benchmark.apply.entries().asScala.toList shouldBe benchmark.rtree.entries().asScala.toList
      benchmark.entries should contain allElementsOf benchmark.rtreeEntries
      benchmark.update.entries().asScala.toList should contain allElementsOf benchmark.rtreeEntries.diff(benchmark.entriesToRemove) ++ benchmark.entriesToAdd
      benchmark.nearest should contain oneElementOf benchmark.rtreeEntries
      benchmark.nearestK.size shouldBe benchmark.nearestMax
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByPoint.size shouldBe 1)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByRectangle.size should be >= 1)
    }
  }
}
