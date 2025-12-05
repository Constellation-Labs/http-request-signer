package io.constellationnetwork.httpsigner.http4s

import java.io.ByteArrayOutputStream

import cats.effect.Async
import cats.implicits._
import org.http4s.{Header, Headers, Request, Response}
import org.typelevel.log4cats.Logger
import io.constellationnetwork.httpsigner.{
  HttpCryptoConfig,
  HttpSigner,
  HttpVerifier,
  SignatureMissing,
  SignatureVerificationResult
}
import org.typelevel.ci.CIString

package object impl {

  trait Helpers {

    protected def md5(msg: Array[Byte]): String = this.synchronized {
      import java.security.MessageDigest

      val md5 = MessageDigest.getInstance("MD5")
      md5.update(msg)
      BigInt(1, md5.digest).toLong.toHexString
    }
  }

  trait RequestHelpers[F[_]] extends Helpers {
    implicit val F: Async[F]
    val config: HttpCryptoConfig

    protected def message(request: Request[F]): F[Array[Byte]] =
      F.delay {
        (List(request.method.name, request.uri.path.toString(), request.uri.query.renderString) ++
          request.headers.headers.collect({
            case header if config.protectedHeaders.exists(_.equals(header.name)) =>
              header.toString()
          })).foldLeft(new ByteArrayOutputStream()) { (buffer, value) =>
          buffer.write(value.getBytes)
          buffer
        }
      }.flatMap(content =>
        request.body.compile.to(Array).map { body =>
          content.write(body)
          content.toByteArray
        }
      )
  }

  trait ResponseHelpers[F[_]] extends Helpers {
    implicit val F: Async[F]
    val config: HttpCryptoConfig

    protected def message(response: Response[F]): F[Array[Byte]] =
      F.delay {
        val buffer = new ByteArrayOutputStream()
        buffer.write(response.status.code)

        response.headers.headers
          .collect({
            case header if config.protectedHeaders.exists(_.equals(header.name)) =>
              header.toString()
          })
          .foldLeft(buffer) { (buffer, value) =>
            buffer.write(value.getBytes)
            buffer
          }
      }.flatMap { content =>
        response.body.compile.to(Array).map { body =>
          content.write(body)
          content.toByteArray
        }
      }
  }

  trait Http4sRequestSigner[F[_]] extends HttpSigner[Request[F], F] with RequestHelpers[F] {
    protected val logger: Logger[F]

    override def sign(request: Request[F]): F[Request[F]] =
      message(request).flatMap { msg =>
        (F.blocking(calculateSignature(msg)) <* F.cede).flatMap { signature =>
          logger
            .debug(s"Signing request ${request.method} ${request.pathInfo} req-hash=${md5(msg)} req-sig=$signature")
            .map(_ =>
              request.withHeaders(
                Headers(
                  Header.Raw(config.signatureHeaderName, signature) :: request.headers.headers
                )
              )
            )
        }
      }
  }

  trait Http4sRequestVerifier[F[_]] extends HttpVerifier[Request[F], F] with RequestHelpers[F] {
    implicit val F: Async[F]
    val config: HttpCryptoConfig
    protected val logger: Logger[F]

    override def verify(request: Request[F]): F[SignatureVerificationResult] =
      request.headers.headers
        .find(_.name.equals(config.signatureHeaderName))
        .map(signature =>
          message(request).flatMap { msg =>
            (F.blocking(verifySignature(msg, signature.value)) <* F.cede).flatMap { result =>
              logger.debug(
                s"Verifying request ${request.method} ${request.pathInfo} req-hash=${md5(msg)} req-sig=${signature.value} result=${result}"
              ).as(result)
            }
          }
        )
        .getOrElse(
          logger
            .debug(s"Verifying request ${request.method} ${request.pathInfo} failed, signature missing")
            .as(SignatureMissing)
        )
  }

  trait Http4sResponseSigner[F[_]] extends HttpSigner[Response[F], F] with ResponseHelpers[F] {
    implicit val F: Async[F]
    val config: HttpCryptoConfig
    protected val logger: Logger[F]

    override def sign(response: Response[F]): F[Response[F]] =
      message(response).flatMap { msg =>
        (F.blocking(calculateSignature(msg)) <* F.cede).flatMap { signature =>
          logger
            .debug(s"Signing response resp-sig=$signature")
            .as(response.withHeaders(response.headers.put(Header.Raw(config.signatureHeaderName, signature))))
        }
      }
  }

  trait Http4sResponseVerifier[F[_]] extends HttpVerifier[Response[F], F] with ResponseHelpers[F] {
    implicit val F: Async[F]
    val config: HttpCryptoConfig
    protected val logger: Logger[F]

    override def verify(response: Response[F]): F[SignatureVerificationResult] =
      response.headers.headers
        .find(_.name.equals(config.signatureHeaderName))
        .map(signature =>
          message(response).flatMap { msg =>
            (F.blocking(verifySignature(msg, signature.value)) <* F.cede).flatMap { result =>
              logger.debug(s"Verifying response resp-hash=${md5(msg)} req-sig=${signature.value} result=${result}")
                .as(result)
            }
          }
        )
        .getOrElse(
          logger
            .debug(s"Verifying response failed, signature missing")
            .as(SignatureMissing)
        )
  }
}
