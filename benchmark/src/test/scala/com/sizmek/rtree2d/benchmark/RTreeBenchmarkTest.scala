package com.sizmek.rtree2d.benchmark

import com.sizmek.rtree2d.core.RTreeEntry
import org.scalatest.{Matchers, WordSpec}

class RTreeBenchmarkTest extends WordSpec with Matchers {
  private val benchmark = new RTreeBenchmark {
    setup()
  }

  "RTreeBenchmark" should {
    "return proper values" in {
      benchmark.apply.entries shouldBe benchmark.rtree.entries
      benchmark.entries should contain allElementsOf benchmark.rtreeEntries
      benchmark.searchFirst shouldBe true
      benchmark.searchAll shouldBe false
      benchmark.searchAndCollectAll shouldBe Seq(RTreeEntry(0.509f, 0.279f, 0.511f, 0.281f, PointOfInterest(0.51f, 0.28f)))
    }
  }
}
