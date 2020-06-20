package com.github.plokhotnyuk.rtree2d.benchmark

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RTree2DTest extends AnyWordSpec with Matchers {
  private def benchmark(geom: String, shuffling: Boolean): RTree2D = new RTree2D {
    geometry = geom
    shuffle = shuffling
    setup()
  }
  "RTree2D" should {
    def testWith(benchmark: () => RTree2D): Unit = {
      benchmark().apply.entries shouldBe benchmark().rtree.entries
      benchmark().entries should contain allElementsOf benchmark().rtreeEntries
      benchmark().update.entries should contain allElementsOf benchmark().rtreeEntries.diff(benchmark().entriesToRemove) ++ benchmark().entriesToAdd
      benchmark().nearest should contain oneElementOf benchmark().rtreeEntries
      val b = benchmark()
      (1 to b.size * 2).foreach(_ => b.nearestK.size shouldBe b.nearestMax)
      (1 to b.size * 2).foreach(_ => b.searchByPoint.size shouldBe 1)
      (1 to b.size * 2).foreach(_ => b.searchByRectangle.size should be >= 1)
    }

    "return proper values for plane geometry without shuffling" in testWith(() => benchmark("plane", shuffling = false))
    "return proper values for spherical geometry without shuffling" in testWith(() => benchmark("spherical", shuffling = false))
    "return proper values for plane geometry with shuffling" in testWith(() => benchmark("plane", shuffling = true))
    "return proper values for spherical geometry with shuffling" in testWith(() => benchmark("spherical", shuffling = true))
  }
}
