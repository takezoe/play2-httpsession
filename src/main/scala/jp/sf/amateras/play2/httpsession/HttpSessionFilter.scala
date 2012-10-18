package jp.sf.amateras.play2.httpsession

import scala.collection.JavaConverters._

import javax.servlet._
import javax.servlet.http._
import javax.servlet.annotation._

/**
 * Puts HttpSession into SessionHolder#sessionMap.
 */
@WebFilter(urlPatterns = Array("/*"), asyncSupported = true)
class HttpSessionFilter extends Filter {

  def init(config: FilterConfig): Unit = {}
  def destroy(): Unit = {}

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    request match {
      case httpRequest: HttpServletRequest => {
        val session = httpRequest.getSession()
        try {
          HttpSessionHelpers.setHttpSession(session)
          
          val wrappedRequest = new HttpServletRequestWrapper(httpRequest){
            override def getParameterMap(): java.util.Map[String, Array[String]] =
              (super.getParameterMap().asScala ++ Map("HTTP_SESSION" -> Array(session.getId))).asJava
          }
          chain.doFilter(wrappedRequest, response)
        } finally {
          HttpSessionHelpers.removeHttpSession(session)
        }
      }
    }
  }

}