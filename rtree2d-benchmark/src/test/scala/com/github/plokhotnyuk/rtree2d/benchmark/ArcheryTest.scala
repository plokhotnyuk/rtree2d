package com.github.plokhotnyuk.rtree2d.benchmark

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ArcheryTest extends AnyWordSpec with Matchers {
  def benchmark(shuffling: Boolean): Archery = new Archery {
    shuffle = shuffling
    setup()
  }

  def testWith(benchmark: () => Archery): Unit = {
    benchmark().apply.entries.toSet shouldBe benchmark().rtree.entries.toSet
    benchmark().entries should contain allElementsOf benchmark().rtreeEntries
    benchmark().update.entries.toList should
      contain allElementsOf benchmark().rtreeEntries.diff(benchmark().entriesToRemove) ++ benchmark().entriesToAdd
    benchmark().nearest should contain oneElementOf benchmark().rtreeEntries
    val b = benchmark()
    (1 to b.size * 2).foreach(_ => b.nearestK.size shouldBe b.nearestMax)
    (1 to b.size * 2).foreach(_ => b.searchByPoint.size shouldBe 1)
    (1 to b.size * 2).foreach(_ => b.searchByRectangle.size should be >= 1)
  }

  "Archery" should {
    "return proper values without shuffling" in testWith(() => benchmark(shuffling = false))
    "return proper values with shuffling" in testWith(() => benchmark(shuffling = true))
  }
}
