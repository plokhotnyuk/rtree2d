package com.sizmek.rtree2d.benchmark

import org.scalatest.{Matchers, WordSpec}

class RTree2DTest extends WordSpec with Matchers {
  private def benchmark = new RTree2D {
    setup()
  }
  "RTree2D" should {
    "return proper values" in {
      benchmark.apply.entries.toList shouldBe benchmark.rtree.entries.toList
      benchmark.entries should contain allElementsOf benchmark.rtreeEntries
      benchmark.insert.entries should contain allElementsOf (benchmark.rtreeEntries ++ benchmark.entriesToAddOrRemove)
      benchmark.remove.entries should contain allElementsOf benchmark.rtreeEntries.diff(benchmark.entriesToAddOrRemove)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByPoint.size shouldBe 1)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchByRectangle.size shouldBe 1)
    }
  }
}
