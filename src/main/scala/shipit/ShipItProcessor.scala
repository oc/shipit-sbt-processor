package shipit

import sbt._
import processor.{ProcessorResult, Processor, Success, Reload}

class ShipItProcessor extends Processor {
  def apply(label: String, project: Project, onFailure: Option[String], args: String): ProcessorResult = {
    ShipIt.shipIt(label, project, onFailure, args)
  }

}
