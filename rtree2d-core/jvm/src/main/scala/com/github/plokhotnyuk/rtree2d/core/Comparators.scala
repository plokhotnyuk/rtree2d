package com.github.plokhotnyuk.rtree2d.core

import java.util.Comparator

private[core] class XComparator[A] extends Comparator[RTree[A]] {
  override def compare(t1: RTree[A], t2: RTree[A]): Int =
    java.lang.Float.floatToRawIntBits((t1.minX + t1.maxX) - (t2.minX + t2.maxX))
}

private[core] class YComparator[A] extends Comparator[RTree[A]] {
  override def compare(t1: RTree[A], t2: RTree[A]): Int =
    java.lang.Float.floatToRawIntBits((t1.minY + t1.maxY) - (t2.minY + t2.maxY))
}

