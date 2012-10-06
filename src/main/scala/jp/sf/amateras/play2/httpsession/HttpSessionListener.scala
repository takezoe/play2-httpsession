package jp.sf.amateras.play2.httpsession

import javax.servlet.http._
import javax.servlet.annotation._

/**
 * Removes HttpSession from SessionHolder#sessionMap when a session is destroyed.
 */
@WebListener
class HttpSessionListener extends javax.servlet.http.HttpSessionListener {

  def sessionCreated(event: HttpSessionEvent): Unit = HttpSessionHelpers.setHttpSession(event.getSession())

  def sessionDestroyed(event: HttpSessionEvent): Unit = HttpSessionHelpers.removeHttpSession(event.getSession())

}