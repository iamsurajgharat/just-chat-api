package models

import play.api.libs.json._

sealed trait Chat
case class UserProfile(id: String, name: String)
case class GroupProfile(id: String, name: String, members: List[UserProfile])
final case class SingleChat(member:UserProfile) extends Chat
final case class GroupChat(profile:GroupProfile) extends Chat

object Chat{
    implicit val formatUserProfile:Format[UserProfile] = Json.format[UserProfile]
    implicit val formatGroupProfile:Format[GroupProfile] = Json.format[GroupProfile]
    implicit val formatSingleChat:Format[SingleChat] = Json.format[SingleChat]
    implicit val formatGroupChat:Format[GroupChat] = Json.format[GroupChat]
    implicit val formatChat:Format[Chat] = Json.format[Chat]
}