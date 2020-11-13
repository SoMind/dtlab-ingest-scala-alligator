package somind.dtlab.ingest.actors.functions

import com.fasterxml.jackson.databind.JsonNode
import com.typesafe.scalalogging.LazyLogging
import navicore.data.navipath.dsl.NaviPathSyntax._
import somind.dtlab.ingest.models._

object ExtractTelemetry extends LazyLogging with JsonSupport {

  def extractFromInt(path: String, node: JsonNode): Option[Double] =
    node.query[Int](path).map(_.toDouble)

  def extractFromDouble(path: String, node: JsonNode): Option[Double] =
    node.query[Double](path)

  def extractFromString(path: String, node: JsonNode): Option[Double] =
    node.query[String](path).map(_.toDouble)

  // ejs todo: need an optional outer node to get path info from
  // ejs todo: need an optional outer node to get path info from
  // ejs todo: need an optional outer node to get path info from
  // ejs todo: need an optional outer node to get path info from
  def apply(
      node: JsonNode,
      outerNode: Option[JsonNode],
      extractorSpecs: Seq[TelemetryExtractorSpec]): Seq[(String, Telemetry)] = {
    extractorSpecs.flatMap(extractorSpec => {
      extractorSpec.values.flatMap(value => {
        logger.debug(s"extracting ${value.valueType} from ${value.path}")
        val v: Option[Double] = value.valueType match {
          case "String"  => extractFromString(value.path, node)
          case "string"  => extractFromString(value.path, node)
          case "Int"     => extractFromInt(value.path, node)
          case "int"     => extractFromInt(value.path, node)
          case "Integer" => extractFromInt(value.path, node)
          case "integer" => extractFromInt(value.path, node)
          case "Double"  => extractFromDouble(value.path, node)
          case "double"  => extractFromDouble(value.path, node)
          case _         => extractFromString(value.path, node)
        }
        v match {
          case Some(extractedValue) =>
            extractorSpec.paths.flatMap(pathSeq => {
              CalculatePath(node, outerNode, pathSeq) match {
                case Some(p) =>
                  try {
                    List(
                      (p,
                       Telemetry(value.idx,
                                 extractedValue,
                                 ExtractDatetime(node, extractorSpec))))
                  } catch {
                    case _: java.lang.ClassCastException =>
                      logger.warn(
                        s"can not extract datetime from path ${extractorSpec.datetimePath} from $node")
                      List(
                        (p,
                         Telemetry(value.idx,
                                   extractedValue,
                                   java.time.ZonedDateTime.now()))
                      )
                  }
                case _ =>
                  logger.warn(s"can not extract path from pathspec: $pathSeq")
                  List()
              }
            })
          case _ =>
            logger.debug(s"did not find ${value.path} in input $node")
            List()
        }
      })
    })
  }
}
