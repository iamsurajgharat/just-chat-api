package controllers.v1

import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import javax.inject.Inject
import play.api.mvc.WebSocket
import play.api.libs.streams.ActorFlow
import actors.UserSessionActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow
import actors.UserSessionActor2
import akka.stream.typed.scaladsl.ActorSink
import akka.actor.typed.ActorRef
import akka.NotUsed
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.OverflowStrategies
import akka.stream.OverflowStrategy
import actors.UserSessionManagerActor
import akka.actor.typed.scaladsl.AskPattern._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor.typed.Scheduler
import scala.concurrent.ExecutionContext
import org.joda.time.DateTime

class SessionController @Inject() (
    val controllerComponents: ControllerComponents,
    userSessionManagerActor: ActorRef[
      UserSessionManagerActor.UserSessionManagerCommand
    ]
)(implicit
    system: ActorSystem,
    mat: Materializer,
    ec: ExecutionContext,
    scheduler: Scheduler
) extends BaseController {
  def connect() = Action { _ =>
    val l1 = List(10, 20, 304, 50)
    val h = l1.head
    println("h")
    Ok("Done " + h)
  }

  def socket() = WebSocket.accept[String, String] { request =>
    println("web socket request ")
    ActorFlow.actorRef { out =>
      println("Creating the actor")
      UserSessionActor.props(out)
    }
  }

  def socket2() = WebSocket.accept[String, String] { request =>
    // Log events to the console

    val in = Sink.foreach[String](println)

    // Send a single 'Hello!' message and then leave the socket open
    val out = Source.single("Hello!").concat(Source.maybe)
    val out2 = Source.fromIterator(() => List("10", "20", "30").iterator)

    Flow.fromSinkAndSource(in, out2)
  }

  def socket3() = WebSocket
    .acceptOrResult[
      UserSessionActor2.UserRequest,
      UserSessionActor2.UserResponse
    ] { request =>
      implicit val timeout = Timeout(1.second)
      val n1 = "userActor" + scala.util.Random.nextInt()

      userSessionManagerActor
        .ask(replyTo =>
          UserSessionManagerActor.CreateUserSessionActor(n1, replyTo)
        )
        .map(Right(_))
    }

  def socket4() = WebSocket
    .acceptOrResult[String, String] { request =>
      implicit val timeout = Timeout(1.second)
      val n1 = "userActor" + scala.util.Random.nextInt()

      userSessionManagerActor
        .ask(replyTo =>
          UserSessionManagerActor.CreateUserSessionActor3(n1, replyTo)
        )
        .map(Right(_))
    }
}
