package controllers.v1

import play.api.mvc._
import javax.inject.Inject
import play.api.libs.json.Writes
import play.api.libs.json.JsValue
import play.api.libs.json.JsObject
import play.api.libs.json.JsNumber
import play.api.libs.json.Reads
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.mvc
import play.api.libs.json.JsError
import scala.concurrent.ExecutionContext
import play.api.libs.json.JsPath

class HelloController @Inject() (cc: ControllerComponents)(implicit
    ec: ExecutionContext
) extends AbstractController(cc) {

  def listPlaces = Action {
    val json = Json.toJson(Place.places)
    Ok(json)
  }

  def savePlace = Action(parse.json) { request =>
    val placeResult:JsResult[Place] = request.body.validate[Place]
    placeResult.fold(
      invalid = (errors) => BadRequest(JsError.toJson(errors)),
      valid = (place) => {
        Place.save(place)
        Ok(Json.toJson(place))
      }
    )
  }

  // This helper parses and validates JSON using the implicit `placeReads`
// above, returning errors if the parsed json fails validation.
  def validateJson[A: Reads] = parse.json.validate(
    _.validate[A].asEither.left.map(e => BadRequest(JsError.toJson(e)))
  )

// if we don't care about validation we could replace `validateJson[Place]`
// with `BodyParsers.parse.json[Place]` to get an unvalidated case class
// in `request.body` instead.
  def savePlaceConcise = Action(validateJson[Place]) { request =>
    // `request.body` contains a fully validated `Place` instance.
    val place = request.body
    Place.save(place)
    Ok(Json.obj("message" -> ("Place '" + place.name + "' saved.")))
  }
}

case class Location(lat: Double, long: Double)
case class Place(name: String, location: Location)

object Location {
  implicit val locationWrites: Writes[Location] = new Writes[Location] {
    def writes(o: Location): JsValue = JsObject(
      Seq("lat" -> JsNumber(o.lat), "long" -> JsNumber(o.long))
    )
  }

  implicit val locationReads: Reads[Location] = new Reads[Location] {
    def reads(json: JsValue): JsResult[Location] = {
      val lat = json("lat").as[Double]
      val long = json("long").as[Double]
      JsSuccess(Location(lat, long))
    }
  }
}

object Place {
  var places: List[Place] = {
    List(
      Place("Sandleford", Location(12.45, 78)),
      Place("Watership Down", Location(23, 47.26))
    )
  }

  def save(place: Place): Unit = {
    places = places ::: List(place)
  }

  import play.api.libs.json._

  implicit val placeWrites: Writes[Place] = new Writes[Place] {
    override def writes(o: Place): JsValue = {
      Json.obj(
        "name" -> JsString(o.name),
        "location" -> Json.toJson(o.location)
      )
    }
  }

  implicit val placeReads: Reads[Place] = new Reads[Place] {
    def reads(json: JsValue): JsResult[Place] = {
      val name = json("name").as[String]
      val loc = json("location").as[Location]
      JsSuccess(Place(name, loc))
    }
  }
}
