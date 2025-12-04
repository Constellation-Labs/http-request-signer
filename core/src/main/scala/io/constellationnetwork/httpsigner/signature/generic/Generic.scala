package io.constellationnetwork.httpsigner.signature.generic

import java.security.{KeyPair, PrivateKey, Provider, PublicKey}

import io.constellationnetwork.httpsigner.signature.{GenericGenerator => GGImpl, GenericVerifier => GVImpl}

class Generic(
  override val algorithm: String,
  override val provider: Provider,
  override val privKey: PrivateKey,
  override val pubKey: PublicKey
) extends GVImpl
    with GGImpl

object Generic {

  def apply(algorithm: String, provider: Provider, privKey: PrivateKey, pubKey: PublicKey) =
    new Generic(algorithm, provider, privKey, pubKey)

  def apply(algorithm: String, provider: Provider, keypair: KeyPair) =
    new Generic(algorithm, provider, keypair.getPrivate, keypair.getPublic)
}
