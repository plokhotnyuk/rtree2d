package com.sizmek.rtree2d.benchmark

import org.scalatest.{Matchers, WordSpec}

class JTSTest extends WordSpec with Matchers {
  private def benchmark = new JTS {
    setup()
  }
  "JTS" should {
    "return proper values" in {
      benchmark.apply.entries.toList shouldBe benchmark.rtree.entries.toList
      benchmark.entries should contain allElementsOf benchmark.rtreeEntries
      benchmark.update.entries.toList should contain allElementsOf benchmark.rtreeEntries.diff(benchmark.entriesToRemove) ++ benchmark.entriesToAdd
      benchmark.nearest should contain oneElementOf benchmark.rtreeEntries
      benchmark.nearestK.size shouldBe benchmark.nearestMax
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByPoint.size shouldBe 1)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByRectangle.size should be >= 1)
    }
  }
}
