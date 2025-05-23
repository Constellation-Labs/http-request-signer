package io.constellationnetwork.httpsigner.akkahttp

import akka.stream.Materializer
import cats.effect.Async
import io.constellationnetwork.httpsigner.signature.{Generator, Verifier}
import io.constellationnetwork.httpsigner.HttpCryptoConfig

import scala.language.{higherKinds, postfixOps}

final class AkkaHttpRequestSigner[F[_]](
  override val crypto: Generator,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer, val F: Async[F])
    extends io.constellationnetwork.httpsigner.akkahttp.impl.AkkaHttpRequestSigner[F] {}

final class AkkaHttpRequestVerifier[F[_]](
  override val crypto: Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer, val F: Async[F])
    extends io.constellationnetwork.httpsigner.akkahttp.impl.AkkaHttpRequestVerifier[F] {}

final class AkkaHttpRequestCrypto[F[_]](
  override val crypto: Generator with Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer, val F: Async[F])
    extends io.constellationnetwork.httpsigner.akkahttp.impl.AkkaHttpRequestSigner[F]
    with io.constellationnetwork.httpsigner.akkahttp.impl.AkkaHttpRequestVerifier[F] {}
