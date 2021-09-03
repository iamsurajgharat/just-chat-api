package actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import models.SingleChat
import models.UserProfile
import akka.actor.typed.ActorRef

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
  final case class Ping(text: String) extends UserRequest
  final case class CompleteIn(msg: Option[String]) extends UserRequest
  final case class ErrorIn(err: String) extends UserRequest

  sealed trait UserResponse
  final case class Pong(text: String) extends UserResponse
  final case class Connected(ackMsg: String) extends UserResponse
  final case class PinnedChats(chats: List[models.Chat]) extends UserResponse
  final case class CompleteOut(msg: Option[String]) extends UserResponse
  final case class ErrorOut(err: String) extends UserResponse

  import play.api.libs.json._

  implicit val userRequestConnectFormat = Json.format[Connect]
  implicit val userRequestCompleteInFormat = Json.format[CompleteIn]
  implicit val userRequestErrorInFormat = Json.format[ErrorIn]
  implicit val userRequestPingFormat = Json.format[Ping]
  implicit val userRequestFormat = Json.format[UserRequest]

  implicit val userResponseCompleteOutFormat = Json.format[CompleteOut]
  implicit val userResponsePongFormat = Json.format[Pong]
  implicit val userResponseErrorOutFormat = Json.format[ErrorOut]
  implicit val userResponseConnectedFormat = Json.format[Connected]
  implicit val userResponsePinnedChatFormat = Json.format[PinnedChats]
  implicit val userResponseFormat = Json.format[UserResponse]

  import play.api.mvc.WebSocket.MessageFlowTransformer

  implicit val messageFlowTransformer =
    MessageFlowTransformer.jsonMessageFlowTransformer[UserRequest, UserResponse]

  def apply(
      userProfile: models.UserProfile,
      responseActor: ActorRef[UserResponse]
  ): Behavior[UserRequest] = Behaviors.receive((_, messgae) => {
    messgae match {
      case Connect(userId, _) =>
        println("Received connect :" + userId)
        responseActor ! Connected("Yasss, finally we are connected!")
        sendPinnedChats(responseActor)
        Behaviors.same
      case ErrorIn(err) =>
        println("Received Error :" + err)
        Behaviors.stopped
      case CompleteIn(msg) =>
        println("Received Complete :" + msg)
        Behaviors.stopped
      case Ping(text) => responseActor ! Pong(text)
        Behaviors.same
    }
  }) 

  private def sendPinnedChats(replyTo : ActorRef[UserResponse]):Unit = {
    replyTo ! PinnedChats(
          List(
            SingleChat(UserProfile("abc", "F1 L1")),
            SingleChat(UserProfile("xyz", "F2 L2"))
          )
        )
  }
}
