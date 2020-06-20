package com.github.plokhotnyuk.rtree2d.benchmark

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class JTSTest extends AnyWordSpec with Matchers {
  private def benchmark(shuffling: Boolean): JTS = new JTS {
    shuffle = shuffling
    setup()
  }

  def testWith(benchmark: () => JTS): Unit = {
    benchmark().apply.entries.toList shouldBe benchmark().rtree.entries.toList
    benchmark().entries should contain allElementsOf benchmark().rtreeEntries
    benchmark().update.entries.toList should contain allElementsOf benchmark().rtreeEntries.diff(benchmark().entriesToRemove) ++ benchmark().entriesToAdd
    benchmark().nearest should contain oneElementOf benchmark().rtreeEntries
    val b = benchmark()
    (1 to b.size * 2).foreach(_ => b.nearestK.size shouldBe b.nearestMax)
    (1 to b.size * 2).foreach(_ => b.searchByPoint.size shouldBe 1)
    (1 to b.size * 2).foreach(_ => b.searchByRectangle.size should be >= 1)
  }

  "JTS" should {
    "return proper values without shuffling" in testWith(() => benchmark(shuffling = false))
    "return proper values with shuffling" in testWith(() => benchmark(shuffling = true))
  }
}
