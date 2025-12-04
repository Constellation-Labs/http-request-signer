package io.constellationnetwork.httpsigner

import java.security.{PrivateKey, Provider, PublicKey, Signature}

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.bouncycastle.crypto.signers.RSADigestSigner

package object signature {

  sealed trait Verifier {
    def verify(message: Array[Byte], signature: Array[Byte]): Boolean
  }

  sealed trait Generator {
    def signature(message: Array[Byte]): Array[Byte]
  }

  protected[httpsigner] trait RsaSHA512Verifier extends Verifier {

    val pubKey: AsymmetricKeyParameter
    private val signer = new RSADigestSigner(new SHA512Digest())

    override def verify(message: Array[Byte], signature: Array[Byte]): Boolean = this.synchronized {
      signer.init(false, pubKey)
      signer.update(message, 0, message.length)
      signer.verifySignature(signature)
    }
  }

  protected[httpsigner] trait RsaSHA512Generator extends Generator {

    val privKey: AsymmetricKeyParameter
    private val signer = new RSADigestSigner(new SHA512Digest())

    override def signature(message: Array[Byte]): Array[Byte] = this.synchronized {
      signer.init(true, privKey)
      signer.update(message, 0, message.length)
      signer.generateSignature()
    }
  }

  protected[httpsigner] trait GenericVerifier extends Verifier {

    val algorithm: String
    val provider: Provider
    val pubKey: PublicKey

    private val signer = Signature.getInstance(algorithm, provider)

    override def verify(message: Array[Byte], signature: Array[Byte]): Boolean = this.synchronized {
      signer.initVerify(pubKey)
      signer.update(message)
      signer.verify(signature)
    }
  }

  protected[httpsigner] trait GenericGenerator extends Generator {
    val algorithm: String
    val provider: Provider
    val privKey: PrivateKey

    private val signer = Signature.getInstance(algorithm, provider)

    override def signature(message: Array[Byte]): Array[Byte] = this.synchronized {
      signer.initSign(privKey)
      signer.update(message)
      signer.sign()
    }
  }
}
