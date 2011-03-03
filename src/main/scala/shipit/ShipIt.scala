package shipit

import sbt.processor.{ProcessorResult, Success, Reload}
import sbt.{OpaqueVersion, BasicVersion, Project}
import sbt.Process._
import java.io.File

object ShipIt {

  def shipIt(label: String, project: Project, onFailure: Option[String], args: String): ProcessorResult = {
    def handle(version: BasicVersion) = {

      val repoIsClean = ("git status --porcelain" !! project.log).trim.isEmpty
      val isGit = new File(".git").isDirectory

      def help {
        project.log.info("Available commands:\n" +
                " ship - tags, publishes project and bumps version\n" +
                " tag  - tags version in git\n" +
                " bump - increments version")
      }

      def tag {
        if(isGit) {
          project.log.info("Tagging with git")
          ("git tag v" + version.toString.trim) ! project.log
          "git push --tags" ! project.log
        } else {
          project.log.info("Doesn't seem to be a git repository")
        }
      }

      def ship {
        project.act("publish")
      }

      def bump {
        val nv = nextVersion
        val buildProperties: String = project.envBackingPath.toString
        replaceContentInFile(buildProperties, "project.version=", nv.toString)
        if(isGit) {
          ("git add " + buildProperties ! project.log)
          ("git commit -m [shipit]:bump" ! project.log)
          ("git push" ! project.log)
          project.log.info("Commited and pushed new version")
        }
        project.log.info("Bumped: '" + version + "' to " + nv)
      }


      def nextVersion: BasicVersion = {
        val maj = "^([0-9]+)$".r
        val min = "^([0-9]+)\\.([0-9]+)$".r
        val mic = "^([0-9]+)\\.([0-9]+)\\.([0-9]+).*$".r

        version.toString match {
          case maj(_) => version.incrementMajor
          case min(_, _) => version.incrementMinor
          case mic(_, _, _) => version.incrementMicro
        }
      }

      if (isGit && !repoIsClean) {
        project.log.info("Git repository is not clean. You must check in all files before shipping.")
        new Success(project, None)
      } else {
        val Tag = "tag.*".r
        args match {
          case "tag" => {
            tag
            new Success(project, None)
          }
          case "bump" => {
            bump
            new Reload()
          }
          case "ship" => {
            tag
            ship
            bump
            new Reload()
          }
          case _ => {
            help
            new Success(project, None)
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

    val newBuildProperties:String = Source.fromFile(file).getLines.map(replace(_, key, value)).mkString

    val out = new PrintWriter(new FileWriter(file))
    try {
      out.write(newBuildProperties);
    }
    finally {
      out.close
    }
  }


}

