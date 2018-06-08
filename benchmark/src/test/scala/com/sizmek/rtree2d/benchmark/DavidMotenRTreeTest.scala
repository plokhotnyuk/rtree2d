package com.sizmek.rtree2d.benchmark

import org.scalatest.{Matchers, WordSpec}

import collection.JavaConverters._

class DavidMotenRTreeTest extends WordSpec with Matchers {
  private def benchmark = new DavidMotenRTree {
    setup()
  }
  "DavidMotenRTree" should {
    "return proper values" in {
      benchmark.apply.entries().toBlocking.toIterable.asScala.toSeq shouldBe benchmark.rtree.entries().toBlocking.toIterable.asScala.toSeq
      benchmark.entries should contain allElementsOf benchmark.rtreeEntries
      benchmark.insert.entries().toBlocking.toIterable.asScala.toSeq should contain allElementsOf (benchmark.rtreeEntries ++ benchmark.entriesToAddOrRemove)
      benchmark.remove.entries().toBlocking.toIterable.asScala.toSeq should contain allElementsOf benchmark.rtreeEntries.diff(benchmark.entriesToAddOrRemove)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByPoint.size shouldBe 1)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByRectangle.size shouldBe 1)
    }
  }
}
