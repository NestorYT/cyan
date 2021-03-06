package controllers.admin.prod

import auth.Secured
import com.google.inject.Inject
import dao._
import play.api.data.Form
import play.api.mvc.Controller

import scala.concurrent.{ExecutionContext, Future}

class ProductResponses @Inject() ()
  (implicit ec: ExecutionContext,
    pingsDAO: PingsDAO,
    pingResponsesDAO: PingResponsesDAO,
    responsesDAO: ResponsesDAO,
    productConfigDAO: ProductConfigDAO,
    pingExtrasDAO: PingExtrasDAO,
    plpDAO: ProdLicensePingDAO,
    productsDAO: ProductsDAO) extends Controller with Secured {

  def list(prodId: Int) = SecureAction.async {
    productsDAO.findById(prodId).map {
      case Some(prod) =>
        Ok(views.html.admin.prod_resp_list(prod))
    }
  }

  import play.api.data.Forms._
  val responseForm = Form(
    tuple(
      "license" -> text,
      "user" -> text,
      "response" -> optional(number)
    )
  )

  def insert(prodId: Int) = SecureAction.async { implicit request =>
    responseForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest("Error: " + formWithErrors.toString)), // TODO nicer error display
      pingResponse => {
        val licenseOpt = pingResponse._1 match { case "" => None; case x => Some(x) }
        val userOpt = pingResponse._2 match { case "" => None; case x => Some(x) }
        val respOpt = pingResponse._3

        pingResponsesDAO
          .upsertExactPingResponse(Some(prodId), licenseOpt, userOpt, respOpt)
          .map { x =>
            Redirect(routes.ProductResponses.list(prodId))
          }
      }
    )
  }
}

