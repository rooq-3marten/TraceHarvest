package com.example

import com.example.core.auth.TokenCipher

/** Test double: the Android Keystore is not available on the JVM. Reversible and clearly not plaintext. */
class FakeTokenCipher : TokenCipher {
    override fun encrypt(plain: String): String = "enc:" + plain.reversed()
    override fun decrypt(encoded: String): String? =
        if (encoded.startsWith("enc:")) encoded.removePrefix("enc:").reversed() else null
}
