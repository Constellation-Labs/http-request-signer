package io.constellationnetwork.httpsigner.signature.rsa

import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import io.constellationnetwork.httpsigner.signature.RsaSHA512Generator

final class RsaGenerator(override val privKey: AsymmetricKeyParameter) extends RsaSHA512Generator

object RsaGenerator {
  def apply(priv: AsymmetricKeyParameter) = new RsaGenerator(priv)
}
