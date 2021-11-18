package com.serli.quickies.wasm

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import akka.util.ByteString
import play.api.libs.json.{JsString, JsValue, Json}

import java.nio.file.{Files, Paths}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Wasm() {

  val actorSystem = ActorSystem()
  val mat = Materializer(actorSystem)
  val ec = actorSystem.dispatcher
  val http = Http(actorSystem)

  def router(req: HttpRequest): Future[HttpResponse] = {
    val path = req.uri.path.toString()
    if (path.startsWith("/plugins/")) {
      val scriptName = path.replaceFirst("/plugins/", "").split("/").head
      val subPath = path.replaceFirst("/plugins/", "").replaceFirst(scriptName, "")
      val plugin = ByteString(Files.readAllBytes(Paths.get(s"./${scriptName}")))
      Future {
        val headersIn: Map[String, String] = req.headers.map(h => (h.name(), h.value())).groupBy(_._1).mapValues(_.map(_._2).last)
        val context = ByteString(Json.stringify(Json.obj(
          "query" -> req.uri.query().toMap,
          "method" -> req.method.name(),
          "headers" -> headersIn,
          "path" -> subPath,
        )))
        println(s"context: '${context.utf8String}'")
        Wasmer.script(plugin.toByteBuffer.array(), 0) { wasmerEnv =>
          Try {
            val input = wasmerEnv.input(context.toByteBuffer.array())
            wasmerEnv.execFunction("handle_http_request", Seq(input, context.size))
          } match {
            case Failure(ex) => {
              println(s"error from wasm", ex)
              HttpResponse(500, entity = s"internal_server_error: ${ex.getMessage}")
            }
            case Success(output) => {
              println(s"output from wasm: '${output.utf8String}'")
              val resStr = output.utf8String
              val res = if (resStr.endsWith("Q")) Json.parse(resStr.substring(0, resStr.size - 1)) else Json.parse(resStr)
              val status = (res \ "status").as[Int]
              val headersRes = (res \ "headers").as[Map[String, String]]
              val bodyRes = (res \ "body").as[JsValue] match {
                case JsString(v) => v
                case v => Json.stringify(v)
              }
              val contentType = headersRes.get("Content-Type").orElse(headersRes.get("content-type")).getOrElse("text/plain")
              HttpResponse.apply(
                status = StatusCode.int2StatusCode(status),
                headers = headersRes.filterNot(_._1.toLowerCase() == "content-type").map(h => RawHeader(h._1, h._2)).toList,
                entity = HttpEntity.apply(
                  ContentType.parse(contentType).right.get,
                  ByteString(bodyRes)
                )
              )
            }
          }
        }
      }(ec)
    } else {
      FastFuture.successful(HttpResponse(404, entity = "resource not found!"))
    }
  }

  def run(): Unit = {
    http.newServerAt("0.0.0.0", 9001).withMaterializer(mat).bind(router)
  }
}


object Wasm {
  def main(args: Array[String]): Unit = {
    new Wasm().run()
  }
}