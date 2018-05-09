package com.sizmek.rtree2d.benchmark

import org.scalatest.{Matchers, WordSpec}

class RTreeBenchmarkTest extends WordSpec with Matchers {
  private val benchmark = new RTreeBenchmark {
    setup()
  }

  "RTreeBenchmark" should {
    "return proper values" in {
      benchmark.apply.entries shouldBe benchmark.rtree.entries
      benchmark.entries should contain allElementsOf benchmark.rtreeEntries
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchFirst shouldBe true)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchAll shouldBe false)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchAndCollectAll.size shouldBe 1)
    }
  }
}
