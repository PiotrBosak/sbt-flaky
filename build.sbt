lazy val `sbt-flaky` =
  (project in file("."))
    .enablePlugins(ScriptedPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "org.apache.commons" % "commons-vfs2" % "2.1",
        "com.lihaoyi" %% "scalatags" % "0.11.0",
        "io.circe" %% "circe-core" % "0.14.1",
        "io.circe" %% "circe-generic" % "0.14.1",
        "io.circe" %% "circe-parser" % "0.14.1",
        "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
        "org.scalatest" %% "scalatest" % "3.0.1" % "test"
      ),
      sbtPlugin := true,

    )

name := "sbt-flaky"

scalaVersion := "2.12.15"

publishTo := sonatypePublishToBundle.value
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeProfileName := "io.github.piotrbosak"
publishMavenStyle := true
isSnapshot := false

import sbt.url
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage := Some(url("https://github.com/PiotrBosak/sbt-flaky"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/PiotrBosak/sbt-flaky"),
    "scm:git@github.com:PiotrBosak/sbt-flaky.git"
  )
)

crossPaths := false

developers := List(
  Developer(id = "PiotrBosak", name = "Piotr Bosak", email = "piotrebk8@gmail.com", url = url("https://piotrbosak.github.io"))
)

initialCommands in console :=
  """
    | println("Hello from console")
    | import java.io.File
    | import flaky._
    | val flakyReportDirSuccessful: File = new File("./src/test/resources/flakyTestRuns/successful/target/flaky-report/")
    | val successfulReport: FlakyTestReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1", "2"), flakyReportDirSuccessful)
    | val flakyReportDirWithFailures: File = new File("./src/test/resources/flakyTestRuns/withFailures/target/flaky-report/")
    | val failedReport: FlakyTestReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1", "2", "3"), flakyReportDirWithFailures)
    | val flakyReportDirAllFailures: File = new File("./src/test/resources/flakyTestRuns/allFailures/target/flaky-test-reports/")
    | val flakyReportAllFailures: FlakyTestReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1", "2", "3", "4", "5"), flakyReportDirAllFailures)
    |""".stripMargin

import ReleaseTransformations._

releaseCrossBuild := true
releaseIgnoreUntrackedFiles := true
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies, // check that there are no SNAPSHOT dependencies
  inquireVersions, // ask user to enter the current and next verion
  runClean, // clean
  setReleaseVersion, // set release version in version.sbt
  commitReleaseVersion, // commit the release version
  tagRelease, // create git tag
  releaseStepCommandAndRemaining("+publishSigned"), // run +publishSigned command to sonatype stage release
  setNextVersion, // set next version in version.sbt
  commitNextVersion, // commint next version
  releaseStepCommand("sonatypeRelease"), // run sonatypeRelease and publish to maven central
  pushChanges // push changes to git
)