package actors

import play.api.test.Helpers._
import models.UserProfile
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import akka.actor.typed.scaladsl.AskPattern._
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.Materializer
import actors.UserSessionActor.Ping
import akka.stream.typed.scaladsl.ActorSink

class UserSessionManagerActorSpec
    extends AsyncFlatSpec
    with Matchers
    with BeforeAndAfterAll {

  import scala.concurrent.duration._
  val oneSecond = 1.second
  val testKit = ActorTestKit()
  val probe1 = testKit.createTestProbe[UserSessionActor.UserRequest]()
  implicit val scheduler = testKit.scheduler
  implicit val mat = Materializer.createMaterializer(testKit.internalSystem)

  def userSessionActorMock(
      userProfile: UserProfile,
      responseActor: ActorRef[UserSessionActor.UserResponse]
  ): Behavior[UserSessionActor.UserRequest] = {
    val mockedUserSessionActor1 =
      Behaviors.receiveMessage[UserSessionActor.UserRequest] { msg =>
        msg match {
          case Ping(text) => responseActor ! UserSessionActor.Pong(text)
          case _          =>
        }
        Behaviors.same
      }

    Behaviors.monitor(probe1.ref, mockedUserSessionActor1)
  }

  "UserSessionManagerActor" should "return the stream flow" in {
    val subject =
      testKit.spawn(UserSessionManagerActor(), "UserSessionManagerActor")

    val result = subject.ask(replyTo =>
      UserSessionManagerActor.CreateUserSessionActor(
        UserProfile("", "Bruce Wayne"),
        userSessionActorMock,
        replyTo
      )
    )
    result.map(_ should not be (null))
  }

  "Strem flow" should "send all input messages to user session actor" in {
    val subject =
      testKit.spawn(UserSessionManagerActor(), "UserSessionManagerActor2")

    val msg1 = UserSessionActor.Connect("userId1", "User Name1")
    val msg2 = UserSessionActor.Connect("userId1", "User Name1")

    // create stream flow , send two messages in, and assert the sent messages on other side
    subject
      .ask(replyTo =>
        UserSessionManagerActor.CreateUserSessionActor(
          UserProfile("", "Bruce Wayne"),
          userSessionActorMock,
          replyTo
        )
      )
      // send two messages
      .map(
        Source
          .fromIterator[UserSessionActor.UserRequest](() =>
            List(msg1, msg2).toIterable.iterator
          )
          .via(_)
          .runWith(Sink.ignore)
      )
      // assert first message
      .map(_ =>
        probe1.expectMessageType[UserSessionActor.Connect](
          oneSecond
        ) should be(msg1)
      )
      // assert second message
      .map(_ =>
        probe1.expectMessageType[UserSessionActor.Connect](
          oneSecond
        ) should be(msg2)
      )
  }

  it should "give out messages sent to response actor" in {

    val subject =
      testKit.spawn(UserSessionManagerActor(), "UserSessionManagerActor3")

    val probe2 = testKit.createTestProbe[UserSessionActor.UserResponse]()

    // create stream flow , send two messages in, and assert the sent messages on other side
    subject
      .ask(replyTo =>
        UserSessionManagerActor.CreateUserSessionActor(
          UserProfile("", "Bruce Wayne"),
          userSessionActorMock,
          replyTo
        )
      )
      // send two messages
      .map(
        Source
          .fromIterator[UserSessionActor.UserRequest](() =>
            List(
              UserSessionActor.Ping("ping1"),
              UserSessionActor.Ping("ping2")
            ).toIterable.iterator
          )
          .via(_)
          .runWith(
            ActorSink.actorRef(
              probe2.ref,
              onCompleteMessage = UserSessionActor.CompleteOut(Some("")),
              onFailureMessage = (_) => UserSessionActor.ErrorOut("")
            )
          )
      )
      // assert first message
      .map(_ =>
        probe2.expectMessageType[UserSessionActor.Pong](
          oneSecond
        ) should be(UserSessionActor.Pong("ping1"))
      )
      // assert second message
      .map(_ =>
        probe2.expectMessageType[UserSessionActor.Pong](
          oneSecond
        ) should be(UserSessionActor.Pong("ping2"))
      )

  }

  override def afterAll(): Unit = testKit.shutdownTestKit()
}
