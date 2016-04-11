package io.pathfinder.authentication

import play.api.libs.ws.WS
import play.api.Play
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json.JsValue
import scala.concurrent.{Future, Promise}
import java.util.Base64
import io.pathfinder.models.Application
import io.pathfinder.authentication.AuthenticationStatus._
import java.util.Date
import play.Logger

import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.security.PublicKey
import java.util.Base64

import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jwt.{JWTClaimsSet,SignedJWT}

object AuthServer {
    val connection = Play.current.configuration.getString("AuthServer.connection").get
    val certificate = Play.current.configuration.getString("AuthServer.certificate").get
    val audience = Play.current.configuration.getString("AuthServer.audience").get
    val issuer = Play.current.configuration.getString("AuthServer.issuer").get
    private var authServerVerifier: RSASSAVerifier = null
    private val kf = KeyFactory.getInstance("RSA")

    private def toVerifier(bytes: Array[Byte]): RSASSAVerifier =
        new RSASSAVerifier(kf.generatePublic(new X509EncodedKeySpec(bytes)).asInstanceOf[RSAPublicKey])

    for {
        response <- WS.url(certificate).get()
    } yield {
        try {
            Logger.info("Received auth server key")
            Logger.info(response.body)
            val pub = new String(response.bodyAsBytes, "UTF-8").replaceAll("-----BEGIN PUBLIC KEY-----\n|-----END PUBLIC KEY-----|\n", "");
            val decoded = Base64.getDecoder.decode(pub)
            authServerVerifier = toVerifier(decoded)
        } catch {
            case e: Throwable =>
                Logger.error("Failed to load auth server key", e)
                System.exit(1)
        }
    }

    def connection(appId: String, cId: String, dashboard: Boolean): Future[AuthenticationStatus] = {
        val app = Application.finder.byId(appId)
        if(app == null){
            throw new NoSuchElementException("No Application with id: " + appId)
        }
        val auth_url = if(null == app.auth_url) {
            connection
        } else {
            app.auth_url
        }
        val verifier = if(connection == auth_url) {
            authServerVerifier
        } else {
            toVerifier(app.key)
        }
        for {
            res <- WS.url(app.auth_url).withQueryString("connection_id" -> cId, "application_id" -> appId).get()
        } yield {
            Logger.info(verifier.toString())
            val token = SignedJWT.parse(new String(res.bodyAsBytes,"UTF-8"))
            if(!token.verify(verifier)){
                throw new IllegalArgumentException("Invalid Token, unable to verify signature")
            }
            val claims = token.getJWTClaimsSet
            if(claims.getExpirationTime.before(new Date())){
                throw new IllegalArgumentException("expired token")
            }
            if(!claims.getAudience.contains(audience)){
                throw new IllegalArgumentException(audience + " required in aud claim")
            }
            if(claims.getIssuer() != issuer){
                throw new IllegalArgumentException(issuer + " required in iss claim")
            }
            val status = claims.getStringClaim("status")
            if(status == null){
                throw new IllegalArgumentException("status claim required")
            }
            AuthenticationStatus.withName(status)
        }
    }
}
