package actors

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Flow
import akka.NotUsed
import UserSessionActor._
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
import models._
import akka.actor.typed.scaladsl.ActorContext
object UserSessionManagerActor {

  // actor supported messages
  sealed trait UserSessionManagerCommand
  final case class CreateUserSessionActor(
      userProfile: UserProfile,
      actorBehavior: (
          UserProfile,
          ActorRef[UserResponse]
      ) => Behavior[UserRequest],
      replyTo: ActorRef[Flow[UserRequest, UserResponse, NotUsed]]
  ) extends UserSessionManagerCommand


  // actors behaviour
  def apply(): Behavior[UserSessionManagerCommand] = Behaviors.receive {
    (context, message) =>
      {
        message match {
          case msg @ CreateUserSessionActor(
                userProfile,
                actorBehavior,
                replyTo
              ) =>

            implicit val mat: Materializer = Materializer(context)
            // hubSink can be joined dynamically by any number of upstreams,
            // and it will merge all those into one and send to its downstream which is hubSource here.
            // On the other hand, hubSource can be joined dynamically to any number of downstreams,
            // and it will broadcast whatever it gets from its upstream (which is hubSink here) to these dynamically joined downstreams.
            val (hubSink, hubSource) = MergeHub
              .source[UserResponse](perProducerBufferSize = 16)
              .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
              .run()

            // create a stream source which is based on an actor.
            // It means if we pass a message to that actor, the message would be emitted into the stream from this source
            val actorSource = ActorSource.actorRef[UserResponse](
              completionMatcher = { case CompleteOut(msg) => },
              failureMatcher = { case ErrorOut(err) =>
                new java.lang.Error("Something terrible happened :" + err)
              },
              bufferSize = 8,
              overflowStrategy = OverflowStrategy.fail
            )

            val userResponseActor =
              actorSource.toMat(hubSink)(akka.stream.scaladsl.Keep.left).run()

            val userSessionActor: ActorRef[UserRequest] = context.spawn(
              actorBehavior(msg.userProfile, userResponseActor),
              "userSessionActor3" + msg.userProfile.id
            )

            val sink: Sink[UserRequest, NotUsed] =
              ActorSink.actorRef[UserRequest](
                userSessionActor,
                onCompleteMessage =
                  CompleteIn(Some("Complete signal, probably from client side")),
                onFailureMessage = (err) => {
                  println("Error signal from client :" + err)
                  ErrorIn(err.getMessage())
                }
              )

            replyTo ! Flow.fromSinkAndSource(sink, hubSource)

            Behaviors.same
        }
      }
  }
}
