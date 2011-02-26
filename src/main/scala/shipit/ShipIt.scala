package shipit

import sbt.processor.{ProcessorResult, Success, Reload}
import sbt.{OpaqueVersion, BasicVersion, Project}
import sbt.Process._

object ShipIt {

  def shipIt(label: String, project: Project, onFailure: Option[String], args: String): ProcessorResult = {
    def handle(version: BasicVersion) = {

      val repoIsClean = ("git status --porcelain" !! project.log).trim.isEmpty

      def tag(version: BasicVersion) {
        project.log.info("Tagging with git")
        ("git tag v" + version.toString.trim) ! project.log
        "git push --tags" ! project.log
      }

      def bump(project: Project, version: BasicVersion) {
        val newVersion: BasicVersion = incrementVersion(version)
        val buildProperties: String = project.envBackingPath.toString
        replaceContentInFile(buildProperties, "project.version=", newVersion.toString)
        ("git add " + buildProperties ! project.log)
        ("git commit -m [shipit]:bump" ! project.log)
        ("git push" ! project.log)
        project.log.info("Bumped: '" + version + "' to " + newVersion)
      }

      def ship(project: Project, version: BasicVersion) {
        project.act("publish")
      }

      def incrementVersion(version: BasicVersion): BasicVersion = {
        val maj = "^([0-9]+)$".r
        val min = "^([0-9]+)\\.([0-9]+)$".r
        val mic = "^([0-9]+)\\.([0-9]+)\\.([0-9]+).*$".r

        version.toString match {
          case maj(_) => version.incrementMajor
          case min(_, _) => version.incrementMinor
          case mic(_, _, _) => version.incrementMicro
        }
      }

      if (!repoIsClean) {
        project.log.info("You must check in all files")
        new Success(project, None)
      }
      else {
        args match {
          case "tag" => {
            tag(version)
            new Success(project, None)
          }
          case "bump" => {
            bump(project, version)
            new Reload()
          }

          case "help" => {
            project.log.info("Available commands: tag, bump, ship")
            new Success(project, None)
          }
          case _ => {
            tag(version)
            ship(project, version)
            bump(project, version)
            new Reload()
          }
        }
      }
    }

    project.version match {
      case v: BasicVersion => handle(v)
      case _: OpaqueVersion => new Success(project, Some("Doesn't support opaque versions. Use something like 1[.1[.1[abc]]]"))
    }
  }

  private def replaceContentInFile(fileName: String, key: String, value: String) {
    import java.io.{File, FileWriter, PrintWriter}
    import io.Source

    val file: File = new File(fileName)

    def replace(line: String, startsWith: String, value: String) = {
      if (line.startsWith(startsWith)) startsWith + value + "\n";
      else line;
    }

    val newBuildProperties: String = Source.fromFile(file).getLines.map(replace(_, key, value)).mkString

    val out = new PrintWriter(new FileWriter(file))
    try {

      out.write(newBuildProperties);
    }
    finally {
      out.close
    }
  }


}

