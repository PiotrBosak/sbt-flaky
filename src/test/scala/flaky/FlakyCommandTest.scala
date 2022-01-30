package flaky

import java.io.File

import org.scalatest.{Matchers, WordSpec}
import sbt.FileFilter

class FlakyCommandTest extends WordSpec with Unzip with Matchers {

  private val zippedGitRepo = new File("./src/test/resources", "gitrepo.zip")
  private val unzippedGitDir = new File("target/")

  val log = new DummySbtLogger()

  "FlakyCommandTest" should {

    "createHtmlReports" in {
      //Goal of this test is also to generate report for visual check
      val reportDir = new File("./target/history8/20170523-231535")
      unzip(new File("./src/test/resources/history8/20170523-231535.zip"), reportDir)
      val dirs: Array[String] = reportDir.listFiles(new FileFilter {
        override def accept(pathname: File): Boolean = pathname.isDirectory
      }).map(_.getName)


      val timeDetails = TimeDetails(System.currentTimeMillis() - 9000000L, System.currentTimeMillis())
      val report = Flaky.createReport("Project X", timeDetails, dirs.toList, reportDir)

      unzip(zippedGitRepo, unzippedGitDir)
      val htmlReportDir = new File("./target/example-report")

      new File(htmlReportDir,"index.html").exists shouldBe true
      new File(htmlReportDir,"flaky-report.html").exists shouldBe true
      new File(htmlReportDir,"flaky-report-history.html").exists shouldBe true
    }

  }
}
