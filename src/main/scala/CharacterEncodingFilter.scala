package jp.sf.amateras.play2.httpsession

import javax.servlet.http._
import javax.servlet.http.{HttpSession => ServletSession}
import javax.servlet._
import javax.servlet.annotation._

/**
 * Sets the request character encoding through ServletRequest#setCharacterEncoding().
 * By default, this filter set "UTF-8". You can also configure this encoding in web.xml.
 */
@WebFilter(urlPatterns = Array("/*"))
class CharacterEncodingFilter extends Filter {

  var encoding: String = null

  def init(config: FilterConfig): Unit = {
    encoding = config.getInitParameter("requestEncoding") match {
      case null => "UTF-8"
      case s    => s
    }
  }

  def destroy(): Unit = {}

  def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
    request.setCharacterEncoding(encoding)
    chain.doFilter(request, response)
  }

}