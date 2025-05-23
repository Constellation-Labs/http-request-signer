package io.constellationnetwork.httpsigner.akkahttp

import akka.stream.Materializer
import cats.effect.Async
import io.constellationnetwork.httpsigner.signature.{Generator, Verifier}
import io.constellationnetwork.httpsigner.HttpCryptoConfig

import scala.language.{higherKinds, postfixOps}

final class AkkaHttpResponseSigner[F[_]](
  override val crypto: Generator,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer, val F: Async[F])
    extends io.constellationnetwork.httpsigner.akkahttp.impl.AkkaHttpResponseSigner[F] {}

final class AkkaHttpResponseVerifier[F[_]](
  override val crypto: Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer, val F: Async[F])
    extends io.constellationnetwork.httpsigner.akkahttp.impl.AkkaHttpResponseVerifier[F] {}

final class AkkaHttpResponseCrypto[F[_]](
  override val crypto: Generator with Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val mat: Materializer, val F: Async[F])
    extends io.constellationnetwork.httpsigner.akkahttp.impl.AkkaHttpResponseSigner[F]
    with io.constellationnetwork.httpsigner.akkahttp.impl.AkkaHttpResponseVerifier[F] {}
