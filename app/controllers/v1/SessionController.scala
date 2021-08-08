package controllers.v1

import play.api.mvc.BaseController
import play.api.mvc.ControllerComponents
import javax.inject.Inject

class SessionController @Inject() (
    val controllerComponents: ControllerComponents
) extends BaseController {
  def connect() = Action { _ =>
    Ok("Done")
  }
}
