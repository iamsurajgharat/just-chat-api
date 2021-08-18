package actors

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Flow
import akka.NotUsed
import UserSessionActor2._
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.typed.scaladsl.ActorSource
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Sink
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorSink
import akka.stream.scaladsl.MergeHub
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.BroadcastHub
object UserSessionManagerActor {
  sealed trait UserSessionManagerCommand
  final case class CreateUserSessionActor(
      userId: String,
      replyTo: ActorRef[Flow[UserRequest, UserResponse, NotUsed]]
  ) extends UserSessionManagerCommand
  final case class CreateUserSessionActor3(
      userId: String,
      replyTo: ActorRef[Flow[String, String, NotUsed]]
  ) extends UserSessionManagerCommand

  def apply(): Behavior[UserSessionManagerCommand] = Behaviors.receive {
    (context, message) =>
      {
        message match {
          case CreateUserSessionActor(userId, replyTo) =>
            implicit val mat: Materializer = Materializer(context)

            val (hubSink, hubSource) = MergeHub
              .source[UserResponse](perProducerBufferSize = 16)
              .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
              .run()

            val source1 = ActorSource.actorRef[UserResponse](
              completionMatcher = { case Done2(msg) => },
              failureMatcher = { case Error2(err) =>
                new java.lang.Error("Something terrible happened :"+err)
              },
              bufferSize = 8,
              overflowStrategy = OverflowStrategy.fail
            )

            val userResponseActor =
              source1.toMat(hubSink)(akka.stream.scaladsl.Keep.left).run()

            val userSessionActor: ActorRef[UserRequest] =
              context.spawn(
                UserSessionActor2(userResponseActor),
                "userSessionActor3" + userId
              )
            val sink: Sink[UserRequest, NotUsed] =
              ActorSink.actorRef(
                userSessionActor,
                onCompleteMessage = Done(Some("completed, probably from client side")),
                onFailureMessage = (err) => {
                  println("Error signal from client :" + err)
                  Error(err.getMessage())
                }
              )

            // // Log events to the console
            // val in = Sink.foreach[String](println)

            // // Send a single 'Hello!' message and then leave the socket open
            // val out = Source.single("Hello!").concat(Source.maybe)
            // val out2 = Source.fromIterator(() => List("10", "20", "30").iterator)

            val flow = Flow.fromSinkAndSource(sink, hubSource)
            replyTo ! flow

            Behaviors.same
        }
      }
  }

  def apply2(): Behavior[UserSessionManagerCommand] = Behaviors.receive {
    (context, message) =>
      {
        message match {
          case CreateUserSessionActor3(userId, replyTo) =>
            implicit val mat: Materializer = Materializer(context)

            val (hubSink, hubSource) = MergeHub
              .source[String](perProducerBufferSize = 16)
              .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
              .run()

            val source1 = ActorSource.actorRef[String](
              completionMatcher = { case "Done" => },
              failureMatcher = { case "Exception" =>
                new java.lang.Error("Something terrible happened")
              },
              bufferSize = 8,
              overflowStrategy = OverflowStrategy.fail
            )

            val userResponseActor =
              source1.toMat(hubSink)(akka.stream.scaladsl.Keep.left).run()

            val userSessionActor: ActorRef[String] =
              context.spawn(
                UserSessionActor3(userResponseActor),
                "userSessionActor3" + userId
              )
            val sink: Sink[String, NotUsed] =
              ActorSink.actorRef(
                userSessionActor,
                onCompleteMessage = "Complete",
                onFailureMessage = (err) => {
                  println("Error signal from client :" + err)
                  "Error"
                }
              )

            // // Log events to the console
            // val in = Sink.foreach[String](println)

            // // Send a single 'Hello!' message and then leave the socket open
            // val out = Source.single("Hello!").concat(Source.maybe)
            // val out2 = Source.fromIterator(() => List("10", "20", "30").iterator)

            val flow = Flow.fromSinkAndSource(sink, hubSource)
            replyTo ! flow

            Behaviors.same
        }
      }
  }
}
