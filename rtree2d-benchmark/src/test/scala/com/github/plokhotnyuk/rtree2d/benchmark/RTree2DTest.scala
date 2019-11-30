package com.github.plokhotnyuk.rtree2d.benchmark

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RTree2DTest extends AnyWordSpec with Matchers {
  private def benchmark(geom: String): RTree2D = new RTree2D {
    geometry = geom
    setup()
  }
  "RTree2D" should {
    def testWith(benchmark: () => RTree2D): Unit = {
      benchmark().apply.entries shouldBe benchmark().rtree.entries
      benchmark().entries should contain allElementsOf benchmark().rtreeEntries
      benchmark().update.entries should contain allElementsOf benchmark().rtreeEntries.diff(benchmark().entriesToRemove) ++ benchmark().entriesToAdd
      benchmark().nearest should contain oneElementOf benchmark().rtreeEntries
      benchmark().nearestK.size shouldBe benchmark().nearestMax
      (1 to benchmark().size * 2).foreach(_ => benchmark().searchByPoint.size shouldBe 1)
      (1 to benchmark().size * 2).foreach(_ => benchmark().searchByRectangle.size should be >= 1)
    }

    "return proper values for plane geometry" in testWith(() => benchmark("plane"))
    "return proper values for spherical geometry" in testWith(() => benchmark("spherical"))
  }
}
