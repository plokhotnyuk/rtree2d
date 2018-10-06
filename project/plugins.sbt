addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.3")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.9")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.3.4")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0-M3")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.3.0")
libraryDependencies ++= Seq(
  "org.jfree" % "jfreechart" % "1.5.0",
  "com.github.plokhotnyuk.jsoniter-scala" %% "macros" % "0.28.0"
)
