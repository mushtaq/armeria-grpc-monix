/*
 * Copyright 2020 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package top

import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.common.scalapb.ScalaPbJsonMarshaller
import com.linecorp.armeria.server.Server
import com.linecorp.armeria.server.docs.{DocService, DocServiceFilter}
import com.linecorp.armeria.server.grpc.GrpcService
import com.linecorp.armeria.server.logging.LoggingService
import example.armeria.grpc.scala.hello.{HelloRequest, HelloServiceApi}
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import com.linecorp.armeria.scala.implicits.*

import ExecutionContext.Implicits.global

object Main {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val server = newServer(8080, 8443)
    println(server.toString)
    server.closeOnJvmShutdown()
    val value = server.start()
    value.toScala.onComplete(println)
    value.join()
    logger.info(
      "Server has been started. Serving DocService at http://127.0.0.1:{}/docs",
      server.activeLocalPort
    )
  }

  def newServer(httpPort: Int, httpsPort: Int): Server = {
    val exampleRequest = HelloRequest("Armeria")
    val grpcService =
      GrpcService
        .builder()
        .addService(
          HelloServiceApi.bindService(new HelloServiceImpl)(monix.execution.Scheduler.global)
        )
        .supportedSerializationFormats(GrpcSerializationFormats.values)
        .jsonMarshallerFactory(_ => ScalaPbJsonMarshaller())
        .enableUnframedRequests(true)
        .build()

    val serviceName = HelloServiceApi.SERVICE.getName
    Server
      .builder()
      .http(httpPort)
      .https(httpsPort)
      .tlsSelfSigned()
      .decorator(LoggingService.newDecorator())
      .service(grpcService)
      .serviceUnder(
        "/docs",
        DocService
          .builder()
          .exampleRequests(serviceName, "Hello", exampleRequest)
          .exampleRequests(serviceName, "LazyHello", exampleRequest)
          .exampleRequests(serviceName, "BlockingHello", exampleRequest)
          .exclude(DocServiceFilter.ofServiceName(ServerReflectionGrpc.SERVICE_NAME))
          .build()
      )
      .build()
  }
}
