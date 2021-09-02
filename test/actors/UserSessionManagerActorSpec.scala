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

class UserSessionManagerActorSpec
    extends PlaySpec
    with GuiceOneAppPerTest
    with BeforeAndAfterAll
    with Injecting {

    val testKit = ActorTestKit()


    "UserSessionManagerActor" should {
        "shoud return the expected stream flow" in {
            val subject = testKit.spawn(UserSessionManagerActor(), "UserSessionManagerActor")
            implicit val scheduler = testKit.scheduler
            
            val result = subject.ask(replyTo => UserSessionManagerActor.CreateUserSessionActor(UserProfile("", "Bruce Wayne"), replyTo))
            
        }
    }

    override def afterAll():Unit= testKit.shutdownTestKit()
}
