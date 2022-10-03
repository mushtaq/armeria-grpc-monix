package top

import com.linecorp.armeria.client.grpc.GrpcClients
import com.linecorp.armeria.common.SerializationFormat
import com.linecorp.armeria.common.grpc.GrpcSerializationFormats
import com.linecorp.armeria.common.scalapb.ScalaPbJsonMarshaller
import com.linecorp.armeria.server.Server
import example.armeria.grpc.scala.hello.{HelloReply, HelloRequest, HelloServiceApi}

import scala.reflect.ClassTag
import _root_.munit.FunSuite
import com.google.common.base.Stopwatch
import com.linecorp.armeria.scala.implicits.*
import example.armeria.grpc.scala.hello.HelloServiceApi.Stub
import io.grpc.channelz.v1.ChannelzGrpc.ChannelzStub
import io.grpc.stub.{AbstractStub, StreamObserver}
import monix.eval.Task

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import monix.execution.Scheduler.Implicits.global
import monix.execution.schedulers.CanBlock.permit
import monix.reactive.Observable
import monix.execution.Ack.Continue

class HelloServiceTest extends FunSuite:
  var server: Server = _

  override def beforeAll(): Unit =
    server = Main.newServer(0, 0)
    server.start().join()

//  private val formats: mutable.Set[SerializationFormat] = GrpcSerializationFormats.values().asScala
  private val formats: Set[SerializationFormat] = Set(GrpcSerializationFormats.PROTO)

  formats.foreach { format =>
    test(s"get-reply: $format") {
      val helloService = newClient(format)
      val message = helloService.hello(HelloRequest("Armeria")).runSyncUnsafe().message
      assertEquals(message, "Hello, Armeria!")
    }
  }

  formats.foreach { format =>
    test(s"reply-with-delay: $format") {
      val helloService = newClient(format)
      val message = helloService.lazyHello(HelloRequest("Armeria")).runSyncUnsafe().message
      assertEquals(message, "Hello, Armeria!")
    }
  }

  formats.foreach { format =>
    test(s"reply-from-server-side-blocking-call: $format") {
      val helloService = newClient(format)
      val watch = Stopwatch.createStarted()
      val message = helloService.blockingHello(HelloRequest("Armeria")).runSyncUnsafe().message
      assertEquals(message, "Hello, Armeria!")
      assert(watch.elapsed(TimeUnit.SECONDS) >= 3)
    }
  }

  formats.foreach { format =>
    test(s"lots-of-replies: $format") {
      val helloService = newClient(format)
      val obtained = helloService
        .lotsOfReplies(HelloRequest("Armeria"))
        .toListL
        .runSyncUnsafe()
        .map(_.message)
      val expected = (1 to 5).map(i => s"Hello, Armeria! (sequence: $i)").toList
      assertEquals(obtained, expected)
    }
  }

  formats.foreach { format =>
    test(s"send-lot-of-greetings: $format") {
      val names = List("Armeria", "Grpc", "Streaming")
      val helloService = newClient(format)
      val obtained = helloService.lotsOfGreetings(
        Observable
          .fromIterable(names)
          .map(name => HelloRequest(name))
      )
      assertEquals(obtained.runSyncUnsafe().message, toMessage(names.mkString(", ")))
    }
  }

  formats.foreach { format =>
    test(s"bidirectional hello: $format") {
      val names = List("Armeria", "Grpc", "Streaming")
      val completed = new AtomicBoolean()
      val helloService = newClient(format)
      val obtained = helloService
        .bidiHello(
          Observable
            .fromIterable(names)
            .map(name => HelloRequest(name))
        )
      assertEquals(obtained.toListL.runSyncUnsafe().map(_.message), names.map(toMessage))
    }
  }

  private def newClient(
      serializationFormat: SerializationFormat = GrpcSerializationFormats.PROTO
  ): Stub =
    val stub = GrpcClients
      .builder(uri(serializationFormat))
      .jsonMarshallerFactory(_ => ScalaPbJsonMarshaller())
      .build(classOf[ChannelzStub])
    new Stub(stub.getChannel, Task.evalOnce(stub.getCallOptions))

  private def uri(
      serializationFormat: SerializationFormat = GrpcSerializationFormats.PROTO
  ): String =
    s"$serializationFormat+http://127.0.0.1:${server.activeLocalPort()}/"

  def toMessage(name: String): String = s"Hello, $name!"

end HelloServiceTest
