package flaky

import java.io.File
import flaky.FlakyPlugin._
import cats.implicits._
import flaky.report.TextReport
import sbt._
import sbt.internal.util.complete.Parser

object FlakyCommand {

  import sbt.complete.DefaultParsers._

  def flaky: Command = Command("flaky")(_ => conditionParser ~ testCaseParser) {
    (state, args) =>
      val targetDir = Project.extract(state).get(Keys.target)
      val baseDirectory = Project.extract(state).get(Keys.baseDirectory)


      val testReports = new File(targetDir, "test-reports")
      val flakyReportsDir = new File(targetDir, Project.extract(state).get(autoImport.flakyReportsDir))
      val logFiles = Project.extract(state).get(autoImport.flakyAdditionalFiles)
      val logLevelInTask = Project.extract(state).get(autoImport.flakyLogLevelInTask)
      val testKeys: Seq[TaskKey[Unit]] = Project.extract(state).get(autoImport.flakyTask)
      val testOnlyKeys: Seq[InputKey[Unit]] = Project.extract(state).get(autoImport.flakyTaskByName)
      val moveFilesF = moveFiles(flakyReportsDir, testReports, logFiles) _

      def runTasks(state: State, runIndex: Int): Unit = {
        testKeys.foreach { taskKey =>
          val extracted = Project extract state
          import extracted._
          import sbt.Keys.logLevel
          val newState = append(Seq(logLevel in taskKey := logLevelInTask), state)
          Project.runTask(taskKey, newState, checkCycles = true)
        }
      }

      def runInputTasks(state: State, regex: String): Unit = {
        testOnlyKeys.toList.foldLeft(state) {
          case (state, key) =>
            val extracted = Project.extract(state)
            extracted.runInputTask(key, regex, state)
              ._1
        }
      }

      state.log.info(s"Executing flaky command")
      flakyReportsDir.mkdirs()
      val start = System.currentTimeMillis

      val iterationNames = args match {
        case (Times(count), tc) =>
          val inclusive = 1 to count
          for (i <- inclusive) {

            val timeReport = TimeReport(i, System.currentTimeMillis - start)
            state.log.info(s"${scala.Console.GREEN}Test iteration $i finished. ETA: ${timeReport.estimate(count - i)}${scala.Console.RESET}")
          }
          inclusive.map(_.toString).toList
        case (Duration(minutes), tc) =>
          var i = 1
          val end = start + minutes.toLong * 60 * 1000
          while (System.currentTimeMillis < end) {
            tc match {
              case ByName(name) => runInputTasks(state, name)
              case All => runTasks(state, i)
            }

            val timeReport = TimeReport(i, System.currentTimeMillis - start)
            val timeLeft = end - System.currentTimeMillis
            val formattedSeconds = TimeReport.formatSeconds(timeLeft / 1000)
            state.log.info(s"${scala.Console.GREEN}Test iteration $i finished. ETA: ${formattedSeconds}s [${timeReport.estimateCountIn(timeLeft)}] ${scala.Console.RESET}")
            i = i + 1
          }
          (1 to i).map(_.toString).toList
        case (FirstFailure, tc) =>
          var i = 1
          var foundFail = false
          while (!foundFail) {
            tc match {
              case ByName(name) => runInputTasks(state, name)
              case All => runTasks(state, i)
            }
            if (Flaky.isFailed(testReports)) {
              foundFail = true
            }
            i = i + 1
          }
          (1 to i).map(_.toString).toList
      }

      val testCases = args._2 match {
        case ByName(name) => ???
        case All => ???
      }

      val name: String = Project.extract(state).get(sbt.Keys.name)
      val report: FlakyTestReport = Flaky.createReport(name, TimeDetails(start, System.currentTimeMillis()), iterationNames, flakyReportsDir)


      textReport(baseDirectory, flakyReportsDir, report, state.log)


      state
  }


  private def textReport(baseDirectory: File, flakyReportsDir: File, report: FlakyTestReport, log: Logger): Unit = {
    val textReport = TextReport.render(report)
    Io.writeToFile(new File(flakyReportsDir, "report.txt"), textReport)
    log.info(textReport)

  }


  private def conditionParser: Parser[ConditionArgs] = {
    import sbt.complete.DefaultParsers._
    val times = (Space ~> "times=" ~> NatBasic)
      .examples("times=5", "times=25", "times=100")
      .map { a => Times(a) }
    val duration = (Space ~> "duration=" ~> NatBasic)
      .examples("duration=15", "duration=60")
      .map { a => Duration(a.toLong) }
    val firstFailure = (Space ~> "firstFail")
      .examples("firstFail")
      .map { _ => FirstFailure }
    times | duration | firstFailure
  }


  private def testCaseParser: Parser[TestCaseArgs] = {
    import sbt.complete.DefaultParsers._
    val all = (Space ~> "all")
      .examples("all")
      .map(_ => All)

    val byName = (Space ~> "byName" ~> StringBasic)
      .examples("byName=*MyTestSpec")
      .map(s => ByName(s))

    byName | all
  }

  private def moveFiles(reportsDir: File, testReports: File, logFiles: List[File])(iteration: Int): Unit = {
    val iterationDir = new File(reportsDir, s"$iteration")
    if (iterationDir.exists()) {
      iterationDir.delete()
    }
    testReports.renameTo(iterationDir)
    logFiles.foreach(f => f.renameTo(new File(iterationDir, f.getName)))
  }
}

sealed trait ConditionArgs

case class Times(count: Int) extends ConditionArgs

case class Duration(duration: Long) extends ConditionArgs

case object FirstFailure extends ConditionArgs

sealed trait TestCaseArgs

case class ByName(name: String) extends TestCaseArgs

case object All extends TestCaseArgs
