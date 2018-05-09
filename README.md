# RTree2D

[![build status](https://travis-ci.com/Sizmek/rtree2d.svg?branch=master)](https://travis-ci.com/Sizmek/rtree2d)
[![codecov](https://codecov.io/gh/Sizmek/rtree2d/branch/master/graph/badge.svg)](https://codecov.io/gh/Sizmek/rtree2d)

RTree2D is a 2D immutable [R-tree](https://en.wikipedia.org/wiki/R-tree) with 
[STR (Sort-Tile-Recursive)](https://archive.org/details/DTIC_ADA324493) packing.

## How to use

The library is published to JCenter, so please add a resolver for it in your `build.sbt` file or ensure that it is 
already added:

```sbt
resolvers += Resolver.jcenterRepo
```

Add the library to a dependency list:

```sbt
libraryDependencies += "com.sizmek.rtree2d" %% "core" % "0.1.0"
 ```

Add import, create an R-tree, and use it with search by point or rectangle requests:

```scala
import com.sizmek.rtree2d.core._

val poi1 = RTreeEntry(1.0f, 1.0f, 2.0f, 2.0f, "point of interest 1")
val poi2 = RTreeEntry(2.0f, 2.0f, 3.0f, 3.0f, "point of interest 2")
val entries = Seq(poi1, poi2)

val rtree = RTree(entries)

assert(rtree.entries == entries)
assert(rtree.searchAll(0.0f, 0.0f) == Nil)
assert(rtree.searchAll(1.5f, 1.5f) == Seq(poi1))
assert(rtree.searchAll(2.5f, 2.5f) == Seq(poi2))
assert(rtree.searchAll(1.5f, 1.5f, 2.5f, 2.5f).forall(entries.contains))
```

## How to contribute

### Build

To compile, run tests, check coverage for different Scala versions use a command:

```sh
sbt clean +coverage +test +coverageReport +mimaReportBinaryIssues
```

### Run benchmarks

Benchmarks are developed in the separated module using [Sbt plugin](https://github.com/ktoso/sbt-jmh)
for [JMH tool](http://openjdk.java.net/projects/code-tools/jmh/). 

They test build, unbuild, and search requests for the following parameters:
- nodeCapacity (max number of children per node)
- overlap (number of overlap in each 4 directions for bounding box of entires)
- size (number of entries in the rtree)
- shuffle (flag to turn on/off shuffling of entries before rtree building)

Feel free to modify benchmarks and check how it works on your payload, JDK, and Scala versions.

To see throughput with allocation rate run benchmarks with GC profiler using the following command:

```sh
sbt -no-colors clean 'benchmark/jmh:run -prof gc -rf json -rff rtree.json .*Benchmark.*'
```

It will save benchmark report in `benchamrk/rtree.json` file.

Results that are stored in JSON can be easy plotted in [JMH Visualizer](http://jmh.morethan.io/) by drugging & dropping
of your file(s) to the drop zone or using the `source` or `sources` parameters with an HTTP link to your file(s) in the 
URLs: `http://jmh.morethan.io/?source=<link to json file>` or `http://jmh.morethan.io/?sources=<link to json file1>,<link to json file2>`.

### Publish locally

Publish to local Ivy repo:

```sh
sbt publishLocal
```

Publish to local Maven repo:

```sh
sbt publishM2
```

### Release

For version numbering use [Recommended Versioning Scheme](http://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html#recommended-versioning-scheme)
that is used in the Scala ecosystem.

Double check binary and source compatibility, including behavior, and release using the following command (credentials 
are required):

```sh
sbt release
```

Do not push changes to github until promoted artifacts for the new version are not available for download on 
[jCenter](http://jcenter.bintray.com/com/sizmek/rtree2d/core_2.12/)
to avoid binary compatibility check failures in triggered Travis CI builds. 
