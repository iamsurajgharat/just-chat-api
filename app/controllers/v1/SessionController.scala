package controllers.v1

import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import javax.inject.Inject
import play.api.mvc.WebSocket

import akka.actor.ActorSystem
import akka.stream.Materializer



import actors.UserSessionActor

import akka.actor.typed.ActorRef




import actors.UserSessionManagerActor
import akka.actor.typed.scaladsl.AskPattern._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.actor.typed.Scheduler
import scala.concurrent.ExecutionContext


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

  def socket3() = WebSocket.acceptOrResult[
    UserSessionActor.UserRequest,
    UserSessionActor.UserResponse
  ] { request =>
    implicit val timeout = Timeout(1.second)
    "userActor" + scala.util.Random.nextInt()
    val userId = request.queryString.get("userId").get.head
    val name = request.queryString.get("name").get.head
    println("Request user id :" + userId)
    println("Request user name :" + name)

    userSessionManagerActor
      .ask(replyTo =>
        UserSessionManagerActor.CreateUserSessionActor(
          models.UserProfile(userId, name),
          (userProfile, responseActor) =>
            UserSessionActor(userProfile, responseActor),
          replyTo
        )
      )
      .map(Right(_))
  }
}
