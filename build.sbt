import com.typesafe.sbt.pgp.PgpKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import sbt.Keys.scalacOptions
import sbt.url
import scala.sys.process._

lazy val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")

def mimaSettings = mimaDefaultSettings ++ Seq(
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
  }
)

lazy val commonSettings = Seq(
  organization := "com.sizmek.rtree2d",
  organizationHomepage := Some(url("https://sizmek.com")),
  homepage := Some(url("https://github.com/Sizmek/rtree2d")),
  licenses := Seq(("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))),
  startYear := Some(2018),
  developers := List(
    Developer(
      id = "loony-bean",
      name = "Alexey Suslov",
      email = "alexey.suslov@sizmek.com",
      url = url("https://github.com/loony-bean")
    ),
    Developer(
      id = "AnderEnder",
      name = "Andrii Radyk",
      email = "andrii.radyk@sizmek.com",
      url = url("https://github.com/AnderEnder")
    ),
    Developer(
      id = "plokhotnyuk",
      name = "Andriy Plokhotnyuk",
      email = "andriy.plokhotnyuk@sizmek.com",
      url = url("https://twitter.com/aplokhotnyuk")
    ),
  ),
  resolvers += Resolver.jcenterRepo,
  scalaVersion := "2.12.8",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Xfuture",
    "-Xlint"
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, x)) if x >= 12 => Seq("-opt:l:method")
    case Some((2, x)) if x == 11 => Seq("-Ybackend:GenBCode", "-Ydelambdafy:method")
    case _ => Seq()
  }),
  testOptions in Test += Tests.Argument("-oDF"),
  parallelExecution in ThisBuild := false
)

lazy val noPublishSettings = Seq(
  skip in publish := true,
  publishArtifact := false,
  bintrayRelease := ((): Unit),
  bintrayEnsureBintrayPackageExists := ((): Unit),
  bintrayEnsureLicenses := ((): Unit)
)

lazy val publishSettings = Seq(
  bintrayOrganization := Some("sizmek"),
  bintrayRepository := "sizmek-maven",
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/Sizmek/rtree2d"),
      "scm:git@github.com:Sizmek/rtree2d.git"
    )
  ),
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false }
)

lazy val rtree2d = project.in(file("."))
  .aggregate(`rtree2d-core`, `rtree2d-benchmark`)
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)

lazy val `rtree2d-core` = project
  .settings(commonSettings: _*)
  .settings(mimaSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.13.0-M5", "2.13.0-M4", "2.12.8", "2.11.12"),
    libraryDependencies ++= {
      val scalatestV =
        if (scalaVersion.value == "2.13.0-M4") "3.0.6-SNAP2"
        else "3.0.6-SNAP3"
      Seq(
        "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
        "org.scalatest" %% "scalatest" % scalatestV % Test
      )
    }
  )

lazy val `rtree2d-benchmark` = project
  .enablePlugins(JmhPlugin)
  .dependsOn(`rtree2d-core`)
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    crossScalaVersions := Seq("2.12.8", "2.11.12"),
    libraryDependencies ++= {
      val scalatestV =
        if (scalaVersion.value == "2.13.0-M4") "3.0.6-SNAP2"
        else "3.0.6-SNAP3"
      Seq(
        "org.locationtech.jts" % "jts-core" % "1.16.1",
        "com.github.davidmoten" % "rtree" % "0.8.6",
        "org.spire-math" %% "archery" % "0.6.0",
        "pl.project13.scala" % "sbt-jmh-extras" % "0.3.6",
        "org.scalatest" %% "scalatest" % scalatestV % Test
      )
    },
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
