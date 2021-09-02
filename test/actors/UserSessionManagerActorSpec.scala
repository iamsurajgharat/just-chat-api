package actors

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._

import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import akka.actor.testkit.typed.scaladsl.TestInbox
import akka.stream.scaladsl.Flow
import akka.NotUsed
import models.UserProfile
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import org.scalatest.BeforeAndAfterAll
import akka.actor.typed.scaladsl.AskPattern._
import org.scalatest.AsyncFunSpec
import org.scalatest.AsyncFlatSpec
import org.scalatest.Matchers
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import org.scalatest.BeforeAndAfterEach
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.ActorMaterializer
import akka.stream.Materializer
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import actors.UserSessionActor.Ping
import akka.stream.SinkRef
import akka.stream.typed.scaladsl.ActorSink

class UserSessionManagerActorSpec
    extends AsyncFlatSpec
    with Matchers
    with BeforeAndAfterAll {

  val oneSecond = FiniteDuration(1, TimeUnit.SECONDS)
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
    import akka.pattern.pipe
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
              onFailureMessage = (err) => UserSessionActor.ErrorOut("")
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
