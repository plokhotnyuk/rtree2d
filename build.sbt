import org.scalajs.linker.interface.{CheckedBehavior, ESVersion}
import sbt.*

import scala.collection.Seq
import scala.scalanative.build.{GC, LTO, Mode}
import scala.sys.process.*

lazy val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")

lazy val isWindows = System.getProperty("os.name", "").toLowerCase().indexOf("win") >= 0

lazy val commonSettings = Seq(
  organization := "com.github.plokhotnyuk.rtree2d",
  organizationHomepage := Some(url("https://github.com/plokhotnyuk")),
  homepage := Some(url("https://github.com/plokhotnyuk/rtree2d")),
  licenses := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))),
  startYear := Some(2018),
  developers := List(
    Developer(
      id = "loony-bean",
      name = "Alexey Suslov",
      email = "alexey.suslov@gmail.com",
      url = url("https://github.com/loony-bean")
    ),
    Developer(
      id = "AnderEnder",
      name = "Andrii Radyk",
      email = "ander.ender@gmail.com",
      url = url("https://github.com/AnderEnder")
    ),
    Developer(
      id = "plokhotnyuk",
      name = "Andriy Plokhotnyuk",
      email = "plokhotnyuk@gmail.com",
      url = url("https://github.com/plokhotnyuk")
    )
  ),
  scalaVersion := "2.12.19",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 12)) => Seq("-language:higherKinds")
    case Some((2, 13)) => Seq("-Wnonunit-statement")
    case _ => Seq()
  }),
  Test / testOptions += Tests.Argument("-oDF"),
  ThisBuild / parallelExecution := false,
  publishTo := sonatypePublishToBundle.value,
  sonatypeProfileName := "com.github.plokhotnyuk",
  versionScheme := Some("early-semver"),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/plokhotnyuk/rtree2d"),
      "scm:git@github.com:plokhotnyuk/rtree2d.git"
    )
  ),
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false }
)

lazy val jsSettings = Seq(
  scalacOptions ++= {
    val localSourcesPath = (LocalRootProject / baseDirectory).value.toURI
    val remoteSourcesPath = s"https://raw.githubusercontent.com/plokhotnyuk/rtree2d/${git.gitHeadCommit.value.get}/"
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) => Seq(
        s"-P:scalajs:mapSourceURI:$localSourcesPath->$remoteSourcesPath",
        "-P:scalajs:genStaticForwardersForNonTopLevelObjects"
      )
      case _ => Seq(
        s"-scalajs-mapSourceURI:$localSourcesPath->$remoteSourcesPath",
        "-scalajs-genStaticForwardersForNonTopLevelObjects"
      )
    }
  },
  scalaJSLinkerConfig ~= {
    _.withSemantics({
        _.optimized
          .withProductionMode(true)
          .withAsInstanceOfs(CheckedBehavior.Unchecked)
          .withStringIndexOutOfBounds(CheckedBehavior.Unchecked)
          .withArrayIndexOutOfBounds(CheckedBehavior.Unchecked)
          .withArrayStores(CheckedBehavior.Unchecked)
          .withNegativeArraySizes(CheckedBehavior.Unchecked)
          .withNullPointers(CheckedBehavior.Unchecked)
      }).withClosureCompiler(true)
      .withESFeatures(_.withESVersion(ESVersion.ES2015))
      .withModuleKind(ModuleKind.CommonJSModule)
  },
  coverageEnabled := false // FIXME: Unexpected crash of scalac
)

lazy val nativeSettings = Seq(
  scalacOptions ++= Seq("-P:scalanative:genStaticForwardersForNonTopLevelObjects"),
  nativeConfig ~= {
    _.withMode(Mode.releaseFast) // TODO: test with `Mode.releaseSize` and `Mode.releaseFull`
      .withLTO(LTO.none)
      .withGC(GC.immix)
  },
  coverageEnabled := false // FIXME: Unexpected linking error
)

lazy val noPublishSettings = Seq(
  publish / skip := true,
  mimaPreviousArtifacts := Set()
)

lazy val publishSettings = Seq(
  packageOptions += Package.ManifestAttributes("Automatic-Module-Name" -> moduleName.value),
  mimaCheckDirection := {
    def isPatch = {
      val Array(newMajor, newMinor, _) = version.value.split('.')
      val Array(oldMajor, oldMinor, _) = oldVersion.split('.')
      newMajor == oldMajor && newMinor == oldMinor
    }

    if (isPatch) "both" else "backward"
  },
  mimaPreviousArtifacts := {
    def isCheckingRequired = {
      val Array(newMajor, newMinor, _) = version.value.split('.')
      val Array(oldMajor, oldMinor, _) = oldVersion.split('.')
      newMajor == oldMajor && (newMajor != "0" || newMinor == oldMinor)
    }

    if (isCheckingRequired) Set(organization.value %%% moduleName.value % oldVersion)
    else Set()
  },
  mimaReportSignatureProblems := true
)

lazy val rtree2d = project.in(file("."))
  .aggregate((if (isWindows) winProjects else unixProjects)*)
  .settings(commonSettings)
  .settings(noPublishSettings)

lazy val winProjects = Seq[ProjectReference](`rtree2d-coreJVM`, `rtree2d-coreJS`, `rtree2d-benchmark`)

lazy val unixProjects = Seq[ProjectReference](`rtree2d-coreJVM`, `rtree2d-coreJS`, `rtree2d-coreNative`, `rtree2d-benchmark`)

lazy val `rtree2d-core` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("3.3.3", "2.13.14", "2.12.19"),
    libraryDependencies ++= Seq(
      "org.scalatestplus" %%% "scalacheck-1-18" % "3.2.19.0" % Test,
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    )
  )

lazy val `rtree2d-coreJVM` = `rtree2d-core`.jvm

lazy val `rtree2d-coreJS` = `rtree2d-core`.js
  .settings(jsSettings)

lazy val `rtree2d-coreNative` = `rtree2d-core`.native
  .settings(nativeSettings)

lazy val `rtree2d-benchmark` = project
  .enablePlugins(JmhPlugin)
  .dependsOn(`rtree2d-coreJVM`)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := Seq(scalaVersion.value),
    libraryDependencies ++= Seq(
      "org.locationtech.jts" % "jts-core" % "1.19.0",
      "com.github.davidmoten" % "rtree2" % "0.9.3",
      "org.spire-math" %% "archery" % "0.6.0",
      "org.scalatest" %%% "scalatest" % "3.2.19" % Test
    ),
    charts := Def.inputTaskDyn {
      val jmhParams = Def.spaceDelimited().parsed
      val targetDir = crossTarget.value
      val jmhReport = targetDir / "benchmark.json"
      val runTask = Jmh / run
        Def.inputTask {
        val _ = runTask.evaluated
        Bencharts(jmhReport, "Execution time (ns/op)", targetDir)
        targetDir
      }.toTask(s" -rf json -rff ${jmhReport.absolutePath} ${jmhParams.mkString(" ")}")
    }.evaluated
  )

lazy val charts = inputKey[File]("Runs the benchmarks and produce charts")
