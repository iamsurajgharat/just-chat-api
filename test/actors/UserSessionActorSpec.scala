package actors

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import models.UserProfile
import akka.actor.testkit.typed.scaladsl.BehaviorTestKit
import akka.actor.testkit.typed.scaladsl.TestInbox

class UserSessionActorSpec
    extends FlatSpec
    with Matchers {

  import UserSessionActor._
  val userProfile = UserProfile("u1", "Steve Rogers")

  "UserSessionActor" should "reply by Connected and PinnedChats for Connect request" in {
    // arrange
    val responsActorInbox = TestInbox[UserResponse]()
    val subject = BehaviorTestKit(
      UserSessionActor(userProfile, responsActorInbox.ref),
      userProfile.id
    )

    // act
    subject.run(Connect(userProfile.id, userProfile.name))

    // assure
    val connectedRes = responsActorInbox.receiveMessage()
    val pinnedChatsRes = responsActorInbox.receiveMessage()

    connectedRes shouldBe a[Connected]
    pinnedChatsRes shouldBe a[PinnedChats]
  }

  it should "stop for ErrorIn" in {
    // arrange
    val responsActorInbox = TestInbox[UserResponse]()
    val subject = BehaviorTestKit(
      UserSessionActor(userProfile, responsActorInbox.ref),
      userProfile.id
    )

    // act
    subject.run(ErrorIn("Client hanged"))

    // assure
    subject.isAlive should be(false)
  }

  it should "stop for CompleteIn" in {
    // arrange
    val responsActorInbox = TestInbox[UserResponse]()
    val subject = BehaviorTestKit(
      UserSessionActor(userProfile, responsActorInbox.ref),
      userProfile.id
    )

    // act
    subject.run(CompleteIn(None))

    // assure
    subject.isAlive should be(false)
  }
}
