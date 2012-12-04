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
        HttpSessionHelpers.setHttpSession(session)
        
        val wrappedRequest = new HttpServletRequestWrapper(httpRequest){
          override def getHeader(name: String): String = {
            name match {
              case "X-SESSION-ID" => session.getId
              case _ => super.getHeader(name)
            }
          }
          
          override def getHeaders(name: String): java.util.Enumeration[String] = {
            name match {
              case "X-SESSION-ID" => new Enumeration(Seq(session.getId))
              case _ => super.getHeaders(name)
            }
          }
          
          override def getHeaderNames(): java.util.Enumeration[String] =
            new Enumeration(httpRequest.getHeaderNames.asScala.toSeq :+ "X-SESSION-ID")
        }
        
        chain.doFilter(wrappedRequest, response)
      }
    }
  }

}
