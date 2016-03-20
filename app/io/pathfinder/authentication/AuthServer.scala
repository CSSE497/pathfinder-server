package io.pathfinder.authentication

import play.api.libs.ws.WS
import play.api.Play
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsValue
import scala.concurrent.{Future, Promise}
import io.jsonwebtoken.{Jwts,Claims}
import java.util.Base64
import io.jsonwebtoken.JwtParser
import java.util.Date

object AuthServer {
    private val connectionUrl = Play.current.configuration.getString("authServer.connection").get
    private val certificateUrl = Play.current.configuration.getString("authServer.certificateUrl").get
    private var publicKey: String = null

    for {
        response <- WS.url(certificateUrl).get()
    } yield {
        val str = new String(response.bodyAsBytes,"UTF-8").replaceAll("(-+BEGIN PUBLIC KEY-+\\r?\\n|-+END PUBLIC KEY-+\\r?\\n?)", "")
        publicKey = new String(Base64.getDecoder.decode(str), "UTF-8")
    }

    def connection(id: String): Future[Unit] = for {
        res <- WS.url(connectionUrl).withQueryString("connection_id" -> id).get()
        token = new String(res.bodyAsBytes,"UTF-8")
    } yield {
        val parser = Jwts.parser()
        parser.requireAudience("https://api.thepathfinder.xyz")
        parser.requireAudience(id)
        val claims: Claims = parser.parseClaimsJwt(new String(res.bodyAsBytes,"UTF-8")).getBody
        if(claims.getExpiration.before(new Date())){
            throw new Exception("expired token")
        }
        if(claims.getIssuer == "https://auth.thepathfinder.xyz"){
            parser.setSigningKey(publicKey).parse(token)
        }
    }
}
