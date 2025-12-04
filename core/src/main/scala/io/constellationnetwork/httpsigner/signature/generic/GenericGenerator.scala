package io.constellationnetwork.httpsigner.signature.generic

import java.security.{PrivateKey, Provider}

import io.constellationnetwork.httpsigner.signature.{GenericGenerator => GenericGeneratorImpl}

class GenericGenerator(
  override val algorithm: String,
  override val provider: Provider,
  override val privKey: PrivateKey
) extends GenericGeneratorImpl

object GenericGenerator {
  def apply(algorithm: String, provider: Provider, key: PrivateKey) = new GenericGenerator(algorithm, provider, key)
}
