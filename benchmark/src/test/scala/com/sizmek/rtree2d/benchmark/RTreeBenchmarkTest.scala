package com.sizmek.rtree2d.benchmark

import org.openjdk.jmh.infra.Blackhole
import org.scalatest.{Matchers, WordSpec}

class RTreeBenchmarkTest extends WordSpec with Matchers {
  private val benchmark = new RTreeBenchmark {
    setup()
  }
  private val blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.")

  "RTreeBenchmark" should {
    "return proper values" in {
      benchmark.apply.entries shouldBe benchmark.rtree.entries
      benchmark.entries should contain allElementsOf benchmark.rtreeEntries
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchFirst(blackhole) shouldBe true)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchAll(blackhole) shouldBe false)
      (1 to benchmark.size * 2).foreach(_ => benchmark.searchAndCollectAll.size shouldBe 1)
    }
  }
}
