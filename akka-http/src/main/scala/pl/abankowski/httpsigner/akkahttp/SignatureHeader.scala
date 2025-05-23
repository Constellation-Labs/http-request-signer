package io.constellationnetwork.httpsigner.akkahttp

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import io.constellationnetwork.httpsigner.HttpCryptoConfig

import scala.util.Try

final class SignatureHeader(signature: String) extends ModeledCustomHeader[SignatureHeader] {
  override def renderInRequests = true
  override def renderInResponses = true
  override val companion = SignatureHeader
  override def value: String = signature
}

object SignatureHeader extends ModeledCustomHeaderCompanion[SignatureHeader] with HttpCryptoConfig {
  override val name = signatureHeaderName.toString
  override def parse(value: String) = Try(new SignatureHeader(value))
}
