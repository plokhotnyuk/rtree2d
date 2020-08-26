import sbt._

import scala.sys.process._

lazy val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")

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
  scalaVersion := "2.12.12",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-feature",
    "-unchecked"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, x)) if x == 11 => Seq("-Ybackend:GenBCode", "-Ydelambdafy:method")
    case _ => Seq()
  }) ++ { if (isDotty.value) Seq("-language:Scala2,implicitConversions") else Nil },
  testOptions in Test += Tests.Argument("-oDF"),
  parallelExecution in ThisBuild := false,
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
  skip in publish := true,
  mimaPreviousArtifacts := Set()
)

lazy val publishSettings = Seq(
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

    if (isCheckingRequired) Set(organization.value %% moduleName.value % oldVersion)
    else Set()
  },
  mimaReportSignatureProblems := true
)

lazy val rtree2d = project.in(file("."))
  .aggregate(`rtree2d-coreJVM`, `rtree2d-coreJS`, `rtree2d-benchmark`)
  .settings(commonSettings)
  .settings(noPublishSettings)

lazy val `rtree2d-core` = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Full)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % "3.2.0" % Test
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((0, _)) => Seq(
        ("org.scalatestplus" %%% "scalacheck-1-14" % "3.2.2.0").intransitive().withDottyCompat("0.23.0") % Test,
        ("org.scalacheck" %%% "scalacheck" % "1.14.3").withDottyCompat("0.23.0") % Test
      )
      case _=> Seq(
        "org.scalatestplus" %%% "scalacheck-1-14" % "3.2.2.0" % Test
      )
    })
  )
  .jsSettings(
    crossScalaVersions := Seq("2.13.3", scalaVersion.value, "2.11.12"),
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule).withESFeatures(_.withUseECMAScript2015(false))),
    coverageEnabled := false // FIXME: No support for Scala.js 1.0 yet, see https://github.com/scoverage/scalac-scoverage-plugin/pull/287
  )
  .jvmSettings(
    crossScalaVersions := Seq("0.24.0", "2.13.3", scalaVersion.value, "2.11.12")
  )

lazy val `rtree2d-coreJVM` = `rtree2d-core`.jvm

lazy val `rtree2d-coreJS` = `rtree2d-core`.js

lazy val `rtree2d-benchmark` = project
  .enablePlugins(JmhPlugin)
  .dependsOn(`rtree2d-coreJVM`)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := Seq(scalaVersion.value, "2.11.12"),
    libraryDependencies ++= Seq(
      "org.locationtech.jts" % "jts-core" % "1.17.0",
      "com.github.davidmoten" % "rtree2" % "0.9-RC1",
      "org.spire-math" %% "archery" % "0.6.0",
      "pl.project13.scala" % "sbt-jmh-extras" % "0.3.7",
      "org.openjdk.jmh" % "jmh-core" % "1.25.1",
      "org.openjdk.jmh" % "jmh-generator-asm" % "1.25.1",
      "org.openjdk.jmh" % "jmh-generator-bytecode" % "1.25.1",
      "org.openjdk.jmh" % "jmh-generator-reflection" % "1.25.1",
      "org.scalatest" %% "scalatest" % "3.2.0" % Test
    ),
    charts := Def.inputTaskDyn {
      val jmhParams = Def.spaceDelimited().parsed
      val targetDir = crossTarget.value
      val jmhReport = targetDir / "benchmark.json"
      val runTask = run in Jmh
      Def.inputTask {
        val _ = runTask.evaluated
        Bencharts(jmhReport, "Execution time (ns/op)", targetDir)
        targetDir
      }.toTask(s" -rf json -rff ${jmhReport.absolutePath} ${jmhParams.mkString(" ")}")
    }.evaluated
  )

lazy val charts = inputKey[File]("Runs the benchmarks and produce charts")
