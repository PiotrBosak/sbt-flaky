package flaky

import sbt._

object FlakyPlugin extends AutoPlugin {

  override def requires: Plugins = {
    sbt.plugins.JvmPlugin
  }

  object autoImport {
    val flakyTask: SettingKey[Seq[TaskKey[Unit]]] = settingKey[Seq[TaskKey[Unit]]]("Tasks to run, by default test in Test")
    val flakyTaskByName: SettingKey[Seq[InputKey[Unit]]] = settingKey[Seq[InputKey[Unit]]]("Tests by name")
    val flakyReportsDir: SettingKey[String] = settingKey[String]("Name of folder in target dir to store test reports and additional files")
    val flakyAdditionalFiles: SettingKey[List[File]] = settingKey[List[File]]("List of additional files to backup after test run (for example log files)")
    val flakyLogLevelInTask: SettingKey[sbt.Level.Value] = settingKey[sbt.Level.Value]("SBT logger level for tasks")
    val flakyHistoryDir: SettingKey[Option[File]] = settingKey[Option[File]]("Dir to keep history, for calculating trends")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    Keys.commands += FlakyCommand.flaky,
    flakyTask := Seq(Keys.test in sbt.Test),
    flakyTaskByName := Seq(Keys.testOnly in sbt.Test),
    flakyReportsDir := "flaky-test-reports",
    flakyAdditionalFiles := List.empty[File],
    flakyLogLevelInTask := sbt.Level.Info,
    flakyHistoryDir := None

  )

}
