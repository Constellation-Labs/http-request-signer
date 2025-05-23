package requestsigner

import java.security.SecureRandom

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{
  HttpHeader,
  HttpMethods,
  HttpRequest => AkkaHttpRequest,
  HttpResponse => AkkaHttpResponse,
  Uri => AkkaUri
}
import akka.http.scaladsl.model.Uri.{Host, Path}
import akka.testkit.TestKit
import cats.effect.IO
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.http4s.{Header, Headers, Method, Request, Response, Uri}
import org.http4s.Uri.{Authority, RegName, Scheme}
import org.scalatest.{FunSpec, FunSpecLike, Matchers}
import io.constellationnetwork.httpsigner.signature.rsa.Rsa
import io.constellationnetwork.httpsigner.SignatureValid
import io.constellationnetwork.httpsigner.akkahttp.{AkkaHttpRequestCrypto, AkkaHttpResponseCrypto}
import io.constellationnetwork.httpsigner.http4s.{Http4SResponseCrypto, Http4sRequestCrypto}

import cats.effect.unsafe.implicits.global

class InteropSpec extends TestKit(ActorSystem("MySpec")) with FunSpecLike with Matchers {

  describe("Having all request signers set up") {

    val keySizeBits = 2 ^ 1024
    val strength = 12

    import java.math.BigInteger

    import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
    val publicExponent = BigInteger.valueOf(0x10001)

    val rnd = SecureRandom.getInstanceStrong
    val rsagp = new RSAKeyGenerationParameters(publicExponent, rnd, keySizeBits, strength)

    val rsag = new RSAKeyPairGenerator
    rsag.init(rsagp)

    val crypto = Rsa(rsag.generateKeyPair())

    var signer1: AkkaHttpRequestCrypto[IO] = new AkkaHttpRequestCrypto[IO](crypto)
    var signer2: Http4sRequestCrypto[IO] = new Http4sRequestCrypto[IO](crypto)

    it("they should be compatible") {
      val req = AkkaHttpRequest(
        method = HttpMethods.GET,
        uri = AkkaUri(
          scheme = "http",
          authority = AkkaUri.Authority(host = Host("example.com"), port = 9000),
          path = Path("/foo"),
          queryString = Some("foo=bar")
        ),
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(req).unsafeRunSync()

      val signature1 = signed1.headers.find(_.name == signer1.config.signatureHeaderName.toString)

      val baseUri = Uri.apply(
        Some(Scheme.http),
        Some(Authority(host = RegName("example.com"), port = Some(9000)))
      ).withQueryParam("foo", "bar")

      val req2 = Request[IO](
        method = Method.GET,
        uri = baseUri.withPath("/foo"),
        headers = Headers.of(Header.Raw(signer2.config.signatureHeaderName, signature1.map(_.value()).getOrElse("")))
      )

      val verified = signer2.verify(req2).unsafeRunSync()

      verified shouldEqual SignatureValid
    }
  }

  describe("Having all response signers set up") {

    val keySizeBits = 2 ^ 1024
    val strength = 12

    import java.math.BigInteger

    import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
    val publicExponent = BigInteger.valueOf(0x10001)

    val rnd = SecureRandom.getInstanceStrong
    val rsagp = new RSAKeyGenerationParameters(publicExponent, rnd, keySizeBits, strength)

    val rsag = new RSAKeyPairGenerator
    rsag.init(rsagp)

    val crypto = Rsa(rsag.generateKeyPair())

    var signer1: AkkaHttpResponseCrypto[IO] = new AkkaHttpResponseCrypto[IO](crypto)
    var signer2: Http4SResponseCrypto[IO] = new Http4SResponseCrypto[IO](crypto)

    it("they should be compatible") {
      val res = AkkaHttpResponse(
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(res).unsafeRunSync()

      val signature1 = signed1.headers.find(_.name == signer1.config.signatureHeaderName.toString)

      val res2 = Response[IO](
        headers = Headers.of(Header.Raw(signer1.config.signatureHeaderName, signature1.map(_.value()).getOrElse("")))
      )

      val verified = signer2.verify(res2).unsafeRunSync()

      verified shouldEqual SignatureValid
    }
  }
}
