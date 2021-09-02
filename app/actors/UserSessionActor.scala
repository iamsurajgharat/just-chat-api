package actors

import akka.actor._
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import models.SingleChat
import models.UserProfile

object UserSessionActor {

  /*
    Messages this actor should handle
      1. Connect
      2. Pin chat
      3. Unpinchat
      4. Send Message
      5. Delivered
      6. Red the message

    Messages this actor send to client
      1. list of pinned chats
      2. Kafka messages
      3.
   */

  sealed trait UserRequest
  final case class Connect(userId: String, name: String) extends UserRequest
  final case class Complete(msg: Option[String]) extends UserRequest
  final case class Error(err: String) extends UserRequest

  sealed trait UserResponse
  final case class Connected(ackMsg: String) extends UserResponse
  final case class PinnedChats(chats: List[models.Chat]) extends UserResponse
  final case class Done2(msg: Option[String]) extends UserResponse
  final case class Error2(err: String) extends UserResponse

  import play.api.libs.json._

  implicit val userRequestConnectFormat = Json.format[Connect]
  implicit val userRequestCompleteFormat = Json.format[Complete]
  implicit val userRequestErrorFormat = Json.format[Error]
  implicit val userRequestFormat = Json.format[UserRequest]
  implicit val userResponseDoneFormat = Json.format[Done2]
  implicit val userResponseErrorFormat = Json.format[Error2]
  implicit val userResponseConnectedFormat = Json.format[Connected]
  implicit val userResponsePinnedChatFormat = Json.format[PinnedChats]
  implicit val userResponseFormat = Json.format[UserResponse]

  import play.api.mvc.WebSocket.MessageFlowTransformer

  implicit val messageFlowTransformer =
    MessageFlowTransformer.jsonMessageFlowTransformer[UserRequest, UserResponse]

  def apply(
      userProfile: models.UserProfile,
      responseActor: typed.ActorRef[UserResponse]
  ): Behavior[UserRequest] = Behaviors.receive((context, messgae) => {
    messgae match {
      case Connect(userId, name) =>
        println("Received connect :" + userId)
        responseActor ! Connected("Yasss, finally we are connected!")
        responseActor ! PinnedChats(
          List(
            SingleChat(UserProfile("abc", "F1 L1")),
            SingleChat(UserProfile("xyz", "F2 L2"))
          )
        )
      case Error(err) =>
        println("Received Error :" + err)
      case Complete(msg) =>
        println("Received Complete :" + msg)
    }
    Behaviors.same
  }) 
}
