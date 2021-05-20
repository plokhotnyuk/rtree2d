import sbt._

import scala.sys.process._

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
    ),
  ),
  resolvers += Resolver.sonatypeRepo("staging"),
  scalaVersion := "2.12.13",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((3, _)) => Seq("-language:Scala2,implicitConversions")
    case Some((2, 11)) => Seq("-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8")
    case _ => Seq("-target:jvm-1.8")
  }),
  Test / testOptions += Tests.Argument("-oDF"),
  ThisBuild / parallelExecution := false,
  publishTo := sonatypePublishToBundle.value,
  sonatypeProfileName := "com.github.plokhotnyuk",
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/plokhotnyuk/rtree2d"),
      "scm:git@github.com:plokhotnyuk/rtree2d.git"
    )
  ),
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
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

    def isNewCrossbuild = CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) => true
      case _ => false
    }

    if (isCheckingRequired && !isNewCrossbuild) Set(organization.value %% moduleName.value % oldVersion)
    else Set()
  },
  mimaReportSignatureProblems := true
)

lazy val rtree2d = project.in(file("."))
  .aggregate((if (isWindows) winProjects else unixProjects):_*)
  .settings(commonSettings)
  .settings(noPublishSettings)

lazy val winProjects = Seq[ProjectReference](`rtree2d-coreJVM`, `rtree2d-coreJS`, `rtree2d-benchmark`)

lazy val unixProjects = Seq[ProjectReference](`rtree2d-coreJVM`, `rtree2d-coreJS`, `rtree2d-coreNative`, `rtree2d-benchmark`)

lazy val `rtree2d-core` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq(
        "org.scalatest" %%% "scalatest" % "3.2.4-M1" % Test,
        "org.scalatestplus" %%% "scalacheck-1-15" % "3.2.4.0-M1" % Test
      )
      case _=> Seq(
        "org.scalatest" %%% "scalatest" % "3.2.9" % Test,
        "org.scalatestplus" %%% "scalacheck-1-15" % "3.2.9.0" % Test
      )
    })
  )
  .nativeSettings(
    crossScalaVersions := Seq("2.13.6", scalaVersion.value),
  )
  .jsSettings(
    crossScalaVersions := Seq("3.0.0", "2.13.6", scalaVersion.value, "2.11.12"),
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule).withESFeatures(_.withUseECMAScript2015(false))),
    coverageEnabled := false // FIXME: No support for Scala.js 1.0 yet, see https://github.com/scoverage/scalac-scoverage-plugin/pull/287
  )
  .jvmSettings(
    crossScalaVersions := Seq("3.0.0", "2.13.6", scalaVersion.value, "2.11.12")
  )

lazy val `rtree2d-coreJVM` = `rtree2d-core`.jvm

lazy val `rtree2d-coreJS` = `rtree2d-core`.js

lazy val `rtree2d-coreNative` = `rtree2d-core`.native

lazy val `rtree2d-benchmark` = project
  .enablePlugins(JmhPlugin)
  .dependsOn(`rtree2d-coreJVM`)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),
    libraryDependencies ++= Seq(
      "org.locationtech.jts" % "jts-core" % "1.18.1",
      "com.github.davidmoten" % "rtree2" % "0.9-RC1",
      "org.spire-math" %% "archery" % "0.6.0",
      "org.scalatest" %% "scalatest" % "3.2.9" % Test
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
