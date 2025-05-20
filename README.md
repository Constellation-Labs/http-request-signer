# Http Requests Signer

[![Release](https://github.com/Constellation-Labs/http-request-signer/actions/workflows/release.yml/badge.svg)](https://github.com/Constellation-Labs/http-request-signer/actions/workflows/release.yml)

> This is a fork of [abankowski/http-request-signer](https://github.com/abankowski/http-request-signer), maintained by Constellation Network.

Available implementations for http4s and akka-http are published as separate jars. Both are cross compiled for Scala 2.12 and 2.13.

Current implementation is a draft. Currently only signature with RSA is provided with ease of extensibility.

## Installation

Add the following dependencies to your build.sbt:

```scala
// For the core module
libraryDependencies += "io.constellationnetwork" %% "http-request-signer-core" % "0.1.0"

// For http4s integration
libraryDependencies += "io.constellationnetwork" %% "http4s-request-signer" % "0.1.0"

// For Akka HTTP integration
libraryDependencies += "io.constellationnetwork" %% "akka-http-request-signer" % "0.1.0"
```

## Usage

Provide implicit ContextShift[IO] in the scope. For **akka-http** get also a Materializer.

Prepare a keypair for asymmetric cipher and initialise `Rsa` implementation that includes both signer and verifier. 


    val kp: AsymmetricCipherKeyPair = _
    val rsa = Rsa(kp)

Once you have a cryptography setup you can instantiate and use the request signer:

`val signer = new Http4sRequestSigner(rsa)`

For convenience let’s keep it like this though it’s not really usefull in many cases as you might usually need to have a different private key for signing your requests and different public key for verification of external requests.

On the client side, once you have your request you can inject a signature by calling sign method:

    // http4s
    signer.sign(request: Request[IO]): IO[Request[IO]]
    
    // akka-http
    signer.sign(request: HttpRequest): IO[HttpRequest] 

Signature is injected as a header to the resulting request object and should be transferred to the server. 
It's a proof that url, payload, body and few selected headers have not been changed. It might not work well with proxy (further enhancements are required). 
    
On the server side, grab decoded request:

    // http4s
    signer.verify(request: Request[IO]): IO[SignatureVerificationResult] 
    
    // akka-http
    signer.verify(request: HttpRequest): IO[SignatureVerificationResult]

`SignatureVerificationResult` is defined as below:

    sealed trait SignatureVerificationResult
    case object SignatureValid extends SignatureVerificationResult
    case object SignatureMissing extends SignatureVerificationResult
    case object SignatureMalformed extends SignatureVerificationResult
    case object SignatureInvalid extends SignatureVerificationResult

This method looks for the signature header and verifies it against the request object headers, method, uri and payload.

This project is licensed under the terms of the Apache 2.0 license http://www.apache.org/licenses/LICENSE-2.0_
