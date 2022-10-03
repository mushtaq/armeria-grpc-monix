package top

import com.linecorp.armeria.common.ContextAwareEventLoop
import com.linecorp.armeria.scala.ExecutionContexts.sameThread
import example.armeria.grpc.scala.hello.{HelloReply, HelloRequest, HelloServiceApi}
import io.grpc.stub.StreamObserver
import com.linecorp.armeria.scala.implicits.*
import com.linecorp.armeria.server.ServiceRequestContext
import io.grpc.Metadata
import monix.eval.Task
import monix.execution.Scheduler
import monix.reactive.Observable

import java.util.concurrent.{ScheduledExecutorService, TimeUnit}
import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future, Promise}

class HelloServiceImpl extends HelloServiceApi:
  override def hello(request: HelloRequest, metadata: Metadata): Task[HelloReply] =
    Task.evalOnce(HelloReply(toMessage(request.name)))

  override def lazyHello(request: HelloRequest, metadata: Metadata): Task[HelloReply] =
    Task.sleep(1.second).map(_ => HelloReply(toMessage(request.name)))

  override def blockingHello(request: HelloRequest, metadata: Metadata): Task[HelloReply] =
    Task
      .evalOnce(Thread.sleep(3000))
      .executeOn(Scheduler.io())
      .map(_ => HelloReply(toMessage(request.name)))

  override def lotsOfReplies(request: HelloRequest, metadata: Metadata): Observable[HelloReply] =
    Observable
      .interval(1.second)
      .take(5)
      .map(index => s"Hello, ${request.name}! (sequence: ${index + 1})")
      .map(a => HelloReply(a))

  override def lotsOfGreetings(
      request: Observable[HelloRequest],
      metadata: Metadata
  ): Task[HelloReply] =
    request
      .map(_.name)
      .toListL
      .map(names => HelloReply(toMessage(names.mkString(", "))))

  override def bidiHello(
      request: Observable[HelloRequest],
      metadata: Metadata
  ): Observable[HelloReply] =
    request.map(request => HelloReply(toMessage(request.name)))

  def toMessage(name: String): String = s"Hello, $name!"

end HelloServiceImpl
