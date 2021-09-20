package com.github.plokhotnyuk.rtree2d.core

import org.scalatest.funsuite.AnyFunSuite

class RTreeTest extends AnyFunSuite {
  import EuclideanPlane._

  private val entries = ((1 to 100) :+ 100).map { x =>
    entry(minX = x.toFloat, minY = x.toFloat, maxX = x.toFloat + 1.9f, maxY = x.toFloat + 1.9f, value = x)
  }
  private val rtree = RTree[Int](entries)

  test("RTreeNil.minX") {
    assert(intercept[UnsupportedOperationException](RTree[Int](Nil).minX).getMessage === "RTreeNil.minX")
  }

  test("RTreeNil.minY") {
    assert(intercept[UnsupportedOperationException](RTree[Int](Nil).minY).getMessage === "RTreeNil.minY")
  }

  test("RTreeNil.maxX") {
    assert(intercept[UnsupportedOperationException](RTree[Int](Nil).maxX).getMessage === "RTreeNil.maxX")
  }

  test("RTreeNil.maxY") {
    assert(intercept[UnsupportedOperationException](RTree[Int](Nil).maxY).getMessage === "RTreeNil.maxY")
  }

  test("RTreeNil.entries") {
    assert(RTree[Int](Nil).entries === Seq())
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).entries(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).entries(0)).getMessage === "0")
  }

  test("RTreeNil.nearestOne") {
    assert(RTree[Int](Nil).nearestOption(0, 0) === None)
  }

  test("RTreeNil.nearestK") {
    assert(RTree[Int](Nil).nearestK(0, 0, k = 3) === Seq())
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).nearestK(0, 0, k = 3).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).nearestK(0, 0, k = 3).apply(0)).getMessage === "0")
  }

  test("RTreeNil.searchAll by point") {
    assert(RTree[Int](Nil).searchAll(0, 0) === Seq())
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).searchAll(0, 0).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).searchAll(0, 0).apply(0)).getMessage === "0")
  }

  test("RTreeNil.searchAll by rectangle") {
    assert(RTree[Int](Nil).searchAll(0, 0, 0, 0) === Seq())
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).searchAll(0, 0, 0, 0).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](RTree[Int](Nil).searchAll(0, 0, 0, 0).apply(0)).getMessage === "0")
  }

  test("RTreeNil.equals") {
    assert(RTree[Int](Nil) === RTree[Int](Nil))
    assert(RTree[Int](Nil) === RTree[String](Nil))
    assert(RTree[Int](Nil) !== Seq())
    assert(RTree[Int](Nil) !== entry(1, 2, 1, 2, 5))
    assert(RTree[Int](Nil) !== RTree(Seq(entry(1, 2, 1, 2, 5))))
  }

  test("RTreeNil.hashCode") {
    assert(RTree[Int](Nil).hashCode() === RTree[Int](Nil).hashCode())
    assert(RTree[Int](Nil).hashCode() === RTree[String](Nil).hashCode())
  }

  test("RTreeEntry.entries") {
    assert(entry(1, 2, 1, 2, 5).entries === Seq(entry(1, 2, 5)))
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).entries(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).entries(1)).getMessage === "1")
  }

  test("RTreeEntry.nearestOne") {
    assert(entry(1, 2, 1, 2, 5).nearestOption(0, 0, maxDist = 1f) === None)
    assert(entry(1, 2, 1, 2, 5).nearestOption(0, 0) === Some(entry(1, 2, 1, 2, 5)))
    assert(entry(1, 2, 1, 2, 5).nearestOption(1, 2) === Some(entry(1, 2, 1, 2, 5)))
  }

  test("RTreeEntry.nearestK") {
    assert(entry(1, 2, 1, 2, 5).nearestK(0, 0, k = 3, maxDist = 1f) === Seq())
    assert(entry(1, 2, 1, 2, 5).nearestK(0, 0, k = 0) === Seq())
    assert(entry(1, 2, 1, 2, 5).nearestK(0, 0, k = 3) === Seq(entry(1, 2, 1, 2, 5)))
    assert(entry(1, 2, 1, 2, 5).nearestK(1, 2, k = 3) === Seq(entry(1, 2, 1, 2, 5)))
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).nearestK(1, 2, k = 3).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 1, 2, 5).nearestK(1, 2, k = 3).apply(3)).getMessage === "3")
  }

  test("RTreeEntry.searchAll by point") {
    assert(entry(1, 2, 3, 4, 5).searchAll(0, 0) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(1, 2) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(2, 3) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, Float.NaN) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(Float.NaN, 3) === Seq())
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(1, 2).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(1, 2).apply(1)).getMessage === "1")
  }

  test("RTreeEntry.searchAll by rectangle") {
    assert(entry(1, 2, 3, 4, 5).searchAll(-1, -1, 0, 0) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(2, 3, 4, 5) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4, 5, 6) === Seq(entry(1, 2, 3, 4, 5)))
    assert(entry(1, 2, 3, 4, 5).searchAll(Float.NaN, 4, 5, 6) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(3, Float.NaN, 5, 6) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4, Float.NaN, 6) === Seq())
    assert(entry(1, 2, 3, 4, 5).searchAll(3, 4, 5, Float.NaN) === Seq())
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](entry(1, 2, 3, 4, 5).searchAll(0, 0, 1, 2).apply(1)).getMessage === "1")
  }

  test("RTreeEntry.equals") {
    assert(entry(1, 2, 3, 4, 5) === entry(1, 2, 3, 4, 5))
    assert(entry(1, 2, 3, 4, 5) === RTree(Seq(entry(1, 2, 3, 4, 5))))
    assert(entry(1, 2, 3, 4, 5) !== entry(1.1f, 2, 3, 4, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2.1f, 3, 4, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3.1f, 4, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3, 4.1f, 5))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3, 4, 50))
    assert(entry(1, 2, 3, 4, 5) !== entry(1, 2, 3, 4, "5"))
  }

  test("RTreeEntry.hashCode") {
    assert(entry(1, 2, 3, 4, 5).hashCode() === entry(1, 2, 3, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() === RTree(Seq(entry(1, 2, 3, 4, 5))).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1.1f, 2, 3, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2.1f, 3, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3.1f, 4, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3, 4.1f, 5).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3, 4, 50).hashCode())
    assert(entry(1, 2, 3, 4, 5).hashCode() !== entry(1, 2, 3, 4, "5").hashCode())
  }

  test("RTree.apply") {
    assert(RTree(entries).entries === entries)
    assert(RTree(entries, 4).entries === entries)
    assert(RTree(entries, 8).entries === entries)
    assert(intercept[IllegalArgumentException](RTree(entries, 1)).getMessage ===
      "nodeCapacity should be greater than 1")
  }

  test("RTree.entries") {
    assert(rtree.entries === entries)
    assert(intercept[IndexOutOfBoundsException](rtree.entries(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.entries(entries.length)).getMessage === "101")
  }

  test("RTree.nearestOne") {
    assert(rtree.nearestOption(0, 0) === Some(entries.head))
    assert(rtree.nearestOption(100, 100) === Some(entries.init.init.last))
  }

  test("RTree.nearestK") {
    assert(rtree.nearestK(0, 0, k = 1) === Seq(entries(0)))
    assert(rtree.nearestK(100, 100, k = 1) === Seq(entries.init.init.last))
    assert(rtree.nearestK(0, 0, k = 0) === Seq())
    assert(rtree.nearestK(0, 0, k = 3) === Seq(
      entry(3.0f, 3.0f, 4.9f, 4.9f, 3),
      entry(1.0f, 1.0f, 2.9f, 2.9f, 1),
      entry(2.0f, 2.0f, 3.9f, 3.9f, 2)
    ))
    assert(rtree.nearestK(0, 0, k = 7) === Seq(
      entry(7.0f, 7.0f, 8.9f, 8.9f, 7),
      entry(4.0f, 4.0f, 5.9f, 5.9f, 4),
      entry(6.0f, 6.0f, 7.9f, 7.9f, 6),
      entry(1.0f, 1.0f, 2.9f, 2.9f, 1),
      entry(3.0f, 3.0f, 4.9f, 4.9f, 3),
      entry(2.0f, 2.0f, 3.9f, 3.9f, 2),
      entry(5.0f, 5.0f, 6.9f, 6.9f, 5)
    ))
    assert(intercept[IndexOutOfBoundsException](rtree.nearestK(0, 0, k = 7).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.nearestK(0, 0, k = 7).apply(7)).getMessage === "7")
  }

  test("RTree.update") {
    val (entries1, entries2) = entries.splitAt(entries.size / 2)
    assert(RTree.update(RTree(entries1), Nil, entries2).entries.size === rtree.entries.size)
    assert(RTree.update(rtree, Nil, rtree.entries).entries.size === (rtree.entries ++ rtree.entries).size)
    assert(RTree.update(RTree(Nil), Nil, rtree.entries).entries.size === rtree.entries.size)
    assert(RTree.update(rtree, entries1, entries1).entries.size === entries.size)
    assert(RTree.update(rtree, entries1, Nil).entries.size === entries2.size)
    assert(RTree.update(RTree.update(rtree, Nil, rtree.entries), entries1, Nil).entries.size === (rtree.entries ++ entries2).size)
  }

  test("RTree.searchAll by point") {
    assert(rtree.searchAll(50, 50).map(_.value) === Seq(49, 50))
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50).apply(2)).getMessage === "2")
  }

  test("RTree.search by point") {
    var found = Seq.empty[Int]
    rtree.search(50, 50) { e =>
      found = found :+ e.value
    }
    assert(found === Seq(49, 50))
  }

  test("RTree.searchAll by rectangle") {
    assert(rtree.searchAll(50, 50, 51, 51).map(_.value) === Seq(49, 50, 51))
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50, 51, 51).apply(-1)).getMessage === "-1")
    assert(intercept[IndexOutOfBoundsException](rtree.searchAll(50, 50, 51, 51).apply(3)).getMessage === "3")
  }

  test("RTree.search by rectangle") {
    var found = Seq.empty[Int]
    rtree.search(50, 50, 51, 51) { e =>
      found = found :+ e.value
    }
    assert(found === Seq(49, 50, 51))
  }

  test("RTree.toString") {
    assert(RTree[Int](Nil).toString ===
      """RTreeNil()
        |""".stripMargin)
    assert(RTree[Int](entries.take(1)).toString ===
      s"""RTreeEntry(${1.0f},${1.0f},${2.9f},${2.9f},1)
        |""".stripMargin)
    assert(RTree[Int](entries.take(2)).toString ===
      s"""RTreeNode(${1.0f},${1.0f},${3.9f},${3.9f})
        |  RTreeEntry(${1.0f},${1.0f},${2.9f},${2.9f},1)
        |  RTreeEntry(${2.0f},${2.0f},${3.9f},${3.9f},2)
        |""".stripMargin)
  }

  test("RTree.equals") {
    assert(intercept[UnsupportedOperationException](RTree(entries, 2) == RTree(entries, 4))
      .getMessage === "RTreeNode.equals")
  }

  test("RTree.hashCode") {
    assert(intercept[UnsupportedOperationException](RTree(entries).hashCode())
      .getMessage === "RTreeNode.hashCode")
  }

  test("RTreeEntryBinaryHeap.put") {
    var heap = new RTreeEntryBinaryHeap[Int](Float.PositiveInfinity, 1)
    assert(heap.put(2, entry(0, 0, 2)) == 2)
    assert(heap.put(1, entry(0, 0, 1)) == 1)
    heap = new RTreeEntryBinaryHeap[Int](Float.PositiveInfinity, 2)
    assert(heap.put(2, entry(0, 0, 2)) == Float.PositiveInfinity)
    assert(heap.put(1, entry(0, 0, 1)) == 2)
    heap = new RTreeEntryBinaryHeap[Int](Float.PositiveInfinity, 2)
    assert(heap.put(3, entry(0, 0, 3)) == Float.PositiveInfinity)
    assert(heap.put(2, entry(0, 0, 2)) == 3)
    assert(heap.put(1, entry(0, 0, 1)) == 2)
  }

  test("RTreeEntryBinaryHeap.toIndexedSeq") {
    val heap = new RTreeEntryBinaryHeap[Int](Float.PositiveInfinity, 7)
    heap.put(1, entry(1, 1, 1))
    heap.put(8, entry(8, 8, 8))
    heap.put(2, entry(2, 2, 2))
    heap.put(5, entry(5, 5, 5))
    heap.put(9, entry(9, 9, 9))
    heap.put(6, entry(6, 6, 6))
    heap.put(3, entry(3, 3, 3))
    heap.put(4, entry(4, 4, 4))
    heap.put(0, entry(0, 0, 0))
    heap.put(7, entry(7, 7, 7))
    assert(heap.toIndexedSeq === Seq(
      entry(6, 6, 6),
      entry(5, 5, 5),
      entry(3, 3, 3),
      entry(1, 1, 1),
      entry(4, 4, 4),
      entry(2, 2, 2),
      entry(0, 0, 0)
    ))
  }

  test("issue 176") {
    val entries = List(
      RTreeEntry(721.8336f,-523.8425f,787.8042f,-437.8111f,207126),
      RTreeEntry(-787.8786f,630.1037f,-766.99133f,734.2292f,207127),
      RTreeEntry(-622.9967f,-557.5722f,-552.30176f,-375.27502f,207128),
      RTreeEntry(662.0951f,477.29788f,835.9104f,561.64984f,207129),
      RTreeEntry(674.9127f,83.08402f,697.21155f,194.90155f,207130),
      RTreeEntry(-566.53644f,-318.6589f,-505.5798f,-139.66696f,207131),
      RTreeEntry(958.1979f,-935.0358f,1034.5779f,-775.9738f,207132),
      RTreeEntry(271.69885f,-189.85439f,341.1704f,-153.98232f,207133),
      RTreeEntry(423.90167f,238.51518f,523.7634f,282.67932f,207134),
      RTreeEntry(-119.23047f,945.8102f,59.423004f,1009.839f,207135),
      RTreeEntry(208.23404f,253.91248f,329.4844f,308.18594f,207136),
      RTreeEntry(-600.8326f,316.32135f,-589.0696f,488.44073f,207137),
      RTreeEntry(917.1414f,431.76736f,997.4647f,529.14923f,207138),
      RTreeEntry(-341.83295f,297.6119f,-340.68716f,437.89423f,207139),
      RTreeEntry(-393.00385f,-992.9672f,-292.9598f,-977.8402f,207140),
      RTreeEntry(-282.18173f,-95.33241f,-248.29584f,-88.832596f,207141),
      RTreeEntry(-422.53598f,278.48816f,-231.97588f,421.94073f,207142),
      RTreeEntry(597.0556f,871.05585f,697.70917f,992.1052f,207143),
      RTreeEntry(-266.3053f,-245.75047f,-203.29483f,-55.5065f,207144),
      RTreeEntry(332.2534f,-317.38986f,451.1378f,-270.28586f,207145),
      RTreeEntry(879.86f,-311.47226f,1030.9298f,-307.33218f,207146),
      RTreeEntry(-144.57193f,-812.95276f,-63.529747f,-735.8652f,207147),
      RTreeEntry(-702.87054f,136.89319f,-599.04895f,333.7104f,207148),
      RTreeEntry(-375.89825f,259.5612f,-234.9887f,321.2139f,207149),
      RTreeEntry(-814.5254f,-227.29942f,-683.1148f,-222.56693f,207150))
    RTree(entries, 4).entries.toSet == entries.toSet
  }

  test("issue 292") {
    import SphericalEarth._
    val entries = List(
      RTreeEntry(-21.69785f,21.64581f,-21.69785f,21.64581f,"BWGNZ"),
        RTreeEntry(-18.36536f,21.84219f,-18.36536f,21.84219f,"BWSWX"),
        RTreeEntry(-21.66667f,22.05f,-21.66667f,22.05f,"BWSUN"),
        RTreeEntry(-19.98333f,23.41667f,-19.98333f,23.41667f,"BWMUB"),
        RTreeEntry(-23.9988f,21.77962f,-23.9988f,21.77962f,"BWHUK"),
        RTreeEntry(-26.05f,22.45f,-26.05f,22.45f,"BWTBY"),
        RTreeEntry(-24.60167f,24.72806f,-24.60167f,24.72806f,"BWJWA"),
        RTreeEntry(-25.22435f,25.67728f,-25.22435f,25.67728f,"BWLOQ"),
        RTreeEntry(-19.16422f,23.75201f,-19.16422f,23.75201f,"BWKHW"),
        RTreeEntry(-17.80165f,25.16024f,-17.80165f,25.16024f,"BWBBK"),
        RTreeEntry(-21.3115f,25.37642f,-21.3115f,25.37642f,"BWORP"),
        RTreeEntry(-21.41494f,25.59263f,-21.41494f,25.59263f,"BWLET"),
        RTreeEntry(-24.62694f,25.86556f,-24.62694f,25.86556f,"BWMGS"),
        RTreeEntry(-24.87158f,25.86989f,-24.87158f,25.86989f,"BWRSA"),
        RTreeEntry(-24.65451f,25.90859f,-24.65451f,25.90859f,"BWGBE"),
        RTreeEntry(-24.66667f,25.91667f,-24.66667f,25.91667f,"BWGAB"),
        RTreeEntry(-22.38754f,26.71077f,-22.38754f,26.71077f,"BWSER"),
        RTreeEntry(-23.10275f,26.83411f,-23.10275f,26.83411f,"BWMAH"),
        RTreeEntry(-22.54605f,27.12507f,-22.54605f,27.12507f,"BWPAL"),
        RTreeEntry(-21.97895f,27.84296f,-21.97895f,27.84296f,"BWPKW"),
        RTreeEntry(-21.17f,27.50778f,-21.17f,27.50778f,"BWFRW"))
    val tree = RTree(entries, 4)
    tree.toString == """RTreeNode(-26.05,21.64581,-17.80165,27.84296)
                       |    RTreeNode(-26.05,21.64581,-17.80165,25.91667)
                       |      RTreeNode(-21.69785,21.64581,-18.36536,23.41667)
                       |        RTreeEntry(-21.69785,21.64581,-21.69785,21.64581,BWGNZ)
                       |        RTreeEntry(-18.36536,21.84219,-18.36536,21.84219,BWSWX)
                       |        RTreeEntry(-21.66667,22.05,-21.66667,22.05,BWSUN)
                       |        RTreeEntry(-19.98333,23.41667,-19.98333,23.41667,BWMUB)
                       |      RTreeNode(-26.05,21.77962,-23.9988,25.67728)
                       |        RTreeEntry(-23.9988,21.77962,-23.9988,21.77962,BWHUK)
                       |        RTreeEntry(-26.05,22.45,-26.05,22.45,BWTBY)
                       |        RTreeEntry(-24.60167,24.72806,-24.60167,24.72806,BWJWA)
                       |        RTreeEntry(-25.22435,25.67728,-25.22435,25.67728,BWLOQ)
                       |      RTreeNode(-21.41494,23.75201,-17.80165,25.59263)
                       |        RTreeEntry(-19.16422,23.75201,-19.16422,23.75201,BWKHW)
                       |        RTreeEntry(-17.80165,25.16024,-17.80165,25.16024,BWBBK)
                       |        RTreeEntry(-21.3115,25.37642,-21.3115,25.37642,BWORP)
                       |        RTreeEntry(-21.41494,25.59263,-21.41494,25.59263,BWLET)
                       |      RTreeNode(-24.87158,25.86556,-24.62694,25.91667)
                       |        RTreeEntry(-24.62694,25.86556,-24.62694,25.86556,BWMGS)
                       |        RTreeEntry(-24.87158,25.86989,-24.87158,25.86989,BWRSA)
                       |        RTreeEntry(-24.65451,25.90859,-24.65451,25.90859,BWGBE)
                       |        RTreeEntry(-24.66667,25.91667,-24.66667,25.91667,BWGAB)
                       |    RTreeNode(-23.10275,26.71077,-21.17,27.84296)
                       |      RTreeNode(-23.10275,26.71077,-21.97895,27.84296)
                       |        RTreeEntry(-22.38754,26.71077,-22.38754,26.71077,BWSER)
                       |        RTreeEntry(-23.10275,26.83411,-23.10275,26.83411,BWMAH)
                       |        RTreeEntry(-22.54605,27.12507,-22.54605,27.12507,BWPAL)
                       |        RTreeEntry(-21.97895,27.84296,-21.97895,27.84296,BWPKW)
                       |      RTreeNode(-21.17,27.50778,-21.17,27.50778)
                       |        RTreeEntry(-21.17,27.50778,-21.17,27.50778,BWFRW)
                       |""".stripMargin
    tree.nearestOption(24.65527f, 25.91904f, 50.0f) == Some(RTreeEntry(-24.65451f,25.90859f,-24.65451f,25.90859f,"BWGBE"))
  }

}