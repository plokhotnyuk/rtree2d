package com.sizmek.rtree2d.benchmark

import org.scalatest.{Matchers, WordSpec}

import collection.JavaConverters._

class DavidMotenRTreeTest extends WordSpec with Matchers {
  private def benchmark = new DavidMotenRTree {
    setup()
  }
  "DavidMotenRTree" should {
    "return proper values" in {
      benchmark.apply.entries().toBlocking.toIterable.asScala.toList shouldBe benchmark.rtree.entries().toBlocking.toIterable.asScala.toList
      benchmark.entries should contain allElementsOf benchmark.rtreeEntries
      benchmark.update.entries().toBlocking.toIterable.asScala.toList should contain allElementsOf benchmark.rtreeEntries.diff(benchmark.entriesToRemove) ++ benchmark.entriesToAdd
      benchmark.nearest should contain oneElementOf benchmark.rtreeEntries
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByPoint.size shouldBe 1)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByRectangle.size should be >= 1)
    }
  }
}
