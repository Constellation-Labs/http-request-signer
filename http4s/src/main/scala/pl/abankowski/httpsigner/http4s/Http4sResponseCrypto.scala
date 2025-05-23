package io.constellationnetwork.httpsigner.http4s

import cats.effect.Async
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import io.constellationnetwork.httpsigner.signature.{Generator, Verifier}
import io.constellationnetwork.httpsigner.HttpCryptoConfig

class Http4sResponseSigner[F[_]](
  override val crypto: Generator,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val F: Async[F])
    extends io.constellationnetwork.httpsigner.http4s.impl.Http4sResponseSigner[F] {
  override val logger: Logger[F] = Slf4jLogger.getLogger[F]
}

class Http4sResponseVerifier[F[_]](
  override val crypto: Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val F: Async[F])
    extends io.constellationnetwork.httpsigner.http4s.impl.Http4sResponseVerifier[F] {
  override val logger: Logger[F] = Slf4jLogger.getLogger[F]
}

class Http4SResponseCrypto[F[_]](
  override val crypto: Generator with Verifier,
  override val config: HttpCryptoConfig = new HttpCryptoConfig {}
)(override implicit val F: Async[F])
    extends io.constellationnetwork.httpsigner.http4s.impl.Http4sResponseSigner[F]
    with io.constellationnetwork.httpsigner.http4s.impl.Http4sResponseVerifier[F] {
  override val logger: Logger[F] = Slf4jLogger.getLogger[F]
}
