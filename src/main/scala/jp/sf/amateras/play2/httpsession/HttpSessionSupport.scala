package jp.sf.amateras.play2.httpsession

import com.codahale.jerkson.Json
import play.api.mvc._

/**
 * Provides APIs for Play2 applications.
 * 
 * You can use javax.servlet.http.HttpSession in Play2 applications to import members of this object as following:
 * {{{
 * import jp.sf.amateras.play2.httpsession.HttpSessionSupport._
 * 
 * def index = Action { implicit request =>
 *   val count = (HttpSession[String]("counter") match {
 *     case None    => 0
 *     case Some(i) => i.toInt
 *   }) + 1
 *
 *   Ok("count=%d".format(count)).withHttpSession {
 *     "counter" -> count.toString
 *   }
 * }
 * }}}
 */
object HttpSessionSupport {

  lazy val HttpSession = new HttpSession()

  implicit def withHttpSession[T <: Result](result: T) = new ResultWrapper(result)

  class ResultWrapper(val result: Result) {
    def withHttpSession(session: (String, AnyRef)*)(implicit requestHeader: RequestHeader): Result = {
      HttpSessionHelpers.session.invalidate()
      session.foreach { case (key, value) =>
        if(HttpSessionHelpers.isLocalSession){
          // serializes the session object as JSON in the development mode
          HttpSessionHelpers.session.setAttribute(key, new Json{}.generate(value))
        } else {
          HttpSessionHelpers.session.setAttribute(key, value)
        }
      }
      result
    }

    def withHttpSession(session: HttpSession)(implicit requestHeader: RequestHeader): Result = {
      session.operations.foreach {
        case (key: String) => HttpSessionHelpers.session.removeAttribute(key)
        case (key: String, value: AnyRef) => {
          if(HttpSessionHelpers.isLocalSession){
            // serializes the session object as JSON in the development mode
            HttpSessionHelpers.session.setAttribute(key, new Json{}.generate(value))
          } else {
            HttpSessionHelpers.session.setAttribute(key, value)
          }
        }
      }
      result
    }
  }
}