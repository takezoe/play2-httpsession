play2-httpsession
=================

HttpSession for Play2 applications on the servlet container.

This provides how to access to HttpSession for Play2 applications which works on the servlet container 
using [play2-war-plugin](https://github.com/dlecan/play2-war-plugin).

At first, add the following dependency into your Build.scala:

```scala
resolvers += "amateras-repo" at "http://amateras.sourceforge.jp/mvn/"

libraryDependencies += "jp.sf.amateras.play2.httpsession" %% "play2-httpsession" % "0.0.5"
```

Import ```jp.sf.amateras.play2.httpsession.HttpSessionSupport._``` to access HttpSession.

```scala
import jp.sf.amateras.play2.httpsession.HttpSessionSupport._
 
def index = Action { implicit request =>
  // retrieve the object from HttpSession
  val count = (HttpSession[String]("counter") match {
    case None    => 0
    case Some(i) => i.toInt
  }) + 1

  Ok("count=%d".format(count)).withHttpSession {
    // store objects into HttpSession
    "counter" -> count.toString
  }
}
```

In the Servre 2.5 mode, play2-war-plugin generates web.xml into PROJECT_HOME/war/WEB-INF/ at the initial ```play run``` execution.
Add the following configuration into that web.xml to enable play2-httpsession in the Servlet 2.5 mode.

```xml
<listener>
  <listener-class>jp.sf.amateras.play2.httpsession.HttpSessionListener</listener-class>
</listener>

<filter>
  <filter-name>encoding</filter-name>
  <filter-class>jp.sf.amateras.play2.httpsession.CharacterEncodingFilter</filter-class>
  <init-param>
    <param-name>requestEncoding</param-name>
    <param-value>UTF-8</param-value>
  </init-param>
</filter>

<filter>
  <filter-name>session</filter-name>
  <filter-class>jp.sf.amateras.play2.httpsession.HttpSessionFilter</filter-class>
</filter>

<filter-mapping>
  <filter-name>encoding</filter-name>
  <url-pattern>/*</url-pattern>
</filter-mapping>

<filter-mapping>
  <filter-name>session</filter-name>
  <url-pattern>/*</url-pattern>
</filter-mapping>
```

Release Notes
--------
### 0.0.5 - 05 Dec 2012

* Fixed error by session clearing in the development mode.

### 0.0.4 - 29 Nov 2012

* Use request header instead of cookie to pass the session id.
* When session is cleared, remove all session attributes instead of HttpSession#invalidate().

### 0.0.3 - 10 Nov 2012

* Fix for play2-war-plugin 0.8.1.

### 0.0.2 - 19 Oct 2012

* Fixed bug on the war mode.

### 0.0.1 - 7 Oct 2012

* Initial release of the stand-alone version.
