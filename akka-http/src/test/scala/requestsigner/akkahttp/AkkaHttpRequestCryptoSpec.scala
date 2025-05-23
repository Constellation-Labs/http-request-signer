package requestsigner.akkahttp

import java.security.SecureRandom

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpHeader, HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.model.Uri.Path
import akka.testkit.TestKit
import cats.effect.IO
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.scalatest.{FunSpecLike, Matchers}
import io.constellationnetwork.httpsigner.{
  SignatureInvalid,
  SignatureMissing,
  SignatureValid
}
import io.constellationnetwork.httpsigner.akkahttp.AkkaHttpRequestCrypto
import io.constellationnetwork.httpsigner.signature.rsa.Rsa

import cats.effect.unsafe.implicits.global

class AkkaHttpRequestCryptoSpec
    extends TestKit(ActorSystem("MySpec"))
    with FunSpecLike
    with Matchers {

  describe("Having Http4sRequestSigner set up") {

    val keySizeBits = 2 ^ 1024
    val strength = 12

    import java.math.BigInteger

    import org.bouncycastle.crypto.params.RSAKeyGenerationParameters
    val publicExponent = BigInteger.valueOf(0x10001)

    val rnd = SecureRandom.getInstanceStrong
    val rsagp =
      new RSAKeyGenerationParameters(publicExponent, rnd, keySizeBits, strength)

    val rsag = new RSAKeyPairGenerator
    rsag.init(rsagp)

    val crypto1 = Rsa(rsag.generateKeyPair())
    val crypto2 = Rsa(rsag.generateKeyPair())

    var signer1: AkkaHttpRequestCrypto[IO] =
      new AkkaHttpRequestCrypto[IO](crypto1)
    var signer2: AkkaHttpRequestCrypto[IO] =
      new AkkaHttpRequestCrypto[IO](crypto2)

    it("should generate a signature") {

      val uri = Uri(scheme = "https", path = Path("/foo"))

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      val signed = signer1.sign(req).unsafeRunSync()

      val signature =
        signed.headers.find(_.name == signer1.config.signatureHeaderName.toString)

      signature shouldBe defined

      signature.map(_.value.nonEmpty) shouldEqual Some(true)
    }

    it("different keys give different signature") {
      val uri = Uri(scheme = "https", path = Path("/foo"))

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(req).unsafeRunSync()
      val signed2 = signer2.sign(req).unsafeRunSync()

      val signature1 =
        signed1.headers.find(_.name == signer1.config.signatureHeaderName.toString)
      val signature2 =
        signed2.headers.find(_.name == signer2.config.signatureHeaderName.toString)

      signature1 shouldBe defined
      signature2 shouldBe defined

      signature1.map(_.value) shouldNot equal(signature2.map(_.value))
    }

    it("different uri path gives different signature") {
      val uri1 = Uri(scheme = "https", path = Path("/foo"))

      val req1 = HttpRequest(
        method = HttpMethods.GET,
        uri = uri1,
        headers = List.empty[HttpHeader]
      )

      val uri2 = Uri(scheme = "https", path = Path("/bar"))

      val req2 = HttpRequest(
        method = HttpMethods.GET,
        uri = uri2,
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(req1).unsafeRunSync()
      val signed2 = signer1.sign(req2).unsafeRunSync()

      val signature1 =
        signed1.headers.find(_.name == signer1.config.signatureHeaderName.toString)
      val signature2 =
        signed2.headers.find(_.name == signer1.config.signatureHeaderName.toString)

      signature1 shouldBe defined
      signature2 shouldBe defined

      signature1.map(_.value) shouldNot equal(signature2.map(_.value))
    }

    it("different uri query gives different signature") {
      val uri1 = Uri(scheme = "https", path = Path("/foo"), queryString = Some("bar=baz"))

      val req1 = HttpRequest(
        method = HttpMethods.GET,
        uri = uri1,
        headers = List.empty[HttpHeader]
      )

      val uri2 = Uri(scheme = "https", path = Path("/foo"), queryString = Some("bar=bar"))

      val req2 = HttpRequest(
        method = HttpMethods.GET,
        uri = uri2,
        headers = List.empty[HttpHeader]
      )

      val signed1 = signer1.sign(req1).unsafeRunSync()
      val signed2 = signer1.sign(req2).unsafeRunSync()

      val signature1 =
        signed1.headers.find(_.name == signer1.config.signatureHeaderName.toString)
      val signature2 =
        signed2.headers.find(_.name == signer1.config.signatureHeaderName.toString)

      signature1 shouldBe defined
      signature2 shouldBe defined

      signature1.map(_.value) shouldNot equal(signature2.map(_.value))
    }

    it("should accept valid signature") {
      val uri = Uri(scheme = "https", path = Path("/foo"))

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      val signed = signer1.sign(req).unsafeRunSync()

      signer1.verify(signed).unsafeRunSync() shouldEqual (SignatureValid)
    }

    it("should reject invalid signature") {
      val uri = Uri(scheme = "https", path = Path("/foo"))

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      val signed = signer1.sign(req).unsafeRunSync()

      signer2.verify(signed).unsafeRunSync() shouldEqual (SignatureInvalid)
    }

    it("should not find a signature") {
      val uri = Uri(scheme = "https", path = Path("/foo"))

      val req = HttpRequest(
        method = HttpMethods.GET,
        uri = uri,
        headers = List.empty[HttpHeader]
      )

      signer2.verify(req).unsafeRunSync() shouldEqual (SignatureMissing)
    }
  }
}
