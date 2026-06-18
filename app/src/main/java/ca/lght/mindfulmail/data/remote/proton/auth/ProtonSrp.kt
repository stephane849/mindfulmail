package ca.lght.mindfulmail.data.remote.proton.auth

import android.util.Base64
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Proton SRP-3000 authentication helper.
 *
 * Implements the standard SRP-6a BigInteger math against Proton's 2048-bit prime group
 * (RFC 5054 Appendix A, group 2) with SHA-512 as the hash function.
 *
 * Password hashing (the `x` computation) uses Proton's custom BCrypt-based scheme for
 * auth version 4, which requires the proton-crypto library to be fully functional. A
 * placeholder is provided with a clear error message until that integration is complete.
 *
 * References:
 * - RFC 5054: https://tools.ietf.org/html/rfc5054
 * - Proton SRP spec: https://proton.me/blog/security-updates-2018/
 */
object ProtonSrp {

    // RFC 5054 Appendix A — 2048-bit prime (group 2)
    private val N_HEX =
        "AC6BDB41324A9A9BF166DE5E1389582FAF72B6651987EE07FC3192943DB56050" +
        "A37329CBB4A099ED8193E0757767A13DD52312AB4B03310DCD7F48A9DA04FD50" +
        "E8083969EDB767B0CF6095179A163AB3661A05FBD5FAAAE82918A9962F0B93B8" +
        "55F97993EC975EEAA80D740ADBF4FF747359D041D5C33EA71D281E446B14773B" +
        "CA97B43A23FB801676BD207A436C6481F1D2B9078717461A5B9D32E688F87748" +
        "544523B524B0D57D5EA77A2775D2ECFA032CFBDBF52FB3786160279004E57AE6" +
        "AF874E7303CE53299CCC041C7BC308D82A5698F3A8D0C38271AE35F8E9DBFBB6" +
        "94B5C803D89F7AE435DE236D525F54759B65E372FCD68EF20FA7111F9E4AFF73"

    private val N: BigInteger = BigInteger(N_HEX, 16)
    private val g: BigInteger = BigInteger.valueOf(2)
    private val k: BigInteger by lazy { computeK() }

    private fun sha512(vararg inputs: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-512")
        for (input in inputs) md.update(input)
        return md.digest()
    }

    /** Pad [b] with leading zeros to match [length] bytes. */
    private fun padTo(b: ByteArray, length: Int): ByteArray {
        if (b.size == length) return b
        if (b.size > length) {
            // Strip leading zero byte that BigInteger.toByteArray() may add for sign
            return b.takeLast(length).toByteArray()
        }
        return ByteArray(length - b.size) + b
    }

    private fun bigIntToBytes(n: BigInteger): ByteArray {
        val raw = n.toByteArray()
        // BigInteger.toByteArray() may prepend a 0x00 sign byte — strip it
        return if (raw[0] == 0.toByte()) raw.copyOfRange(1, raw.size) else raw
    }

    /** k = H(N || pad(g)) per SRP-6a */
    private fun computeK(): BigInteger {
        val nBytes = bigIntToBytes(N)
        val gPadded = padTo(bigIntToBytes(g), nBytes.size)
        return BigInteger(1, sha512(nBytes, gPadded))
    }

    /**
     * Hash the user's password according to Proton's auth version scheme.
     *
     * Version 4 (current): BCrypt(SHA512(password), bcryptSalt) where bcryptSalt is
     * derived from the Proton salt. This requires the proton-crypto / bcrypt library.
     *
     * Version 0 (legacy): plain SHA512 of password bytes.
     *
     * @throws NotImplementedError for version 4 until the BCrypt integration is complete.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun hashPassword(password: String, salt: ByteArray, version: Int): ByteArray {
        return when (version) {
            0 -> sha512(password.toByteArray(Charsets.UTF_8))
            else -> throw NotImplementedError(
                "SRP: full BCrypt password hashing (version $version) requires the " +
                "proton-crypto library. See Phase 2 integration notes. " +
                "Integrate the `com.proton.gopenpgp:android-lib` artifact and delegate " +
                "password hashing to its SrpAuth helper before this path is reachable.",
            )
        }
    }

    /**
     * Generate SRP client ephemeral and proof values.
     *
     * @param username  Proton account username (lowercased before hashing).
     * @param password  Plain-text password.
     * @param version   Auth version from [AuthInfoResponse.version].
     * @param salt      Base64-encoded salt from [AuthInfoResponse.salt].
     * @param modulus   Base64-encoded server modulus from [AuthInfoResponse.modulus].
     *                  PGP armoring, if present, must be stripped before calling.
     * @param serverEphemeral Base64-encoded server ephemeral B value.
     * @return [ProtonSrpProofs] containing client ephemeral, client proof, and expected server proof.
     */
    fun generateProofs(
        username: String,
        password: String,
        version: Int,
        salt: String,
        modulus: String,
        serverEphemeral: String,
    ): ProtonSrpProofs {
        val saltBytes = Base64.decode(modulus.trim(), Base64.DEFAULT)
        val modulusBytes = Base64.decode(modulus.trim(), Base64.DEFAULT)
        val B = BigInteger(1, Base64.decode(serverEphemeral.trim(), Base64.DEFAULT))

        // Use N from the modulus bytes (Proton sends its own modulus, not the RFC prime)
        // but the group is still the same prime N — use our constant.
        val nBytes = bigIntToBytes(N)
        val nLen = nBytes.size

        // Password hash — delegates to version-specific logic
        val rawSaltBytes = Base64.decode(salt.trim(), Base64.DEFAULT)
        val passwordHash = hashPassword(password, rawSaltBytes, version)

        // x = H(salt || passwordHash)  — Proton's variant
        val x = BigInteger(1, sha512(rawSaltBytes, passwordHash))

        // Generate random client secret a (256-bit is plenty, N is 2048-bit)
        val secureRandom = SecureRandom()
        var a: BigInteger
        var A: BigInteger
        do {
            a = BigInteger(256, secureRandom)
            A = g.modPow(a, N)
        } while (A.mod(N) == BigInteger.ZERO)

        // u = H(pad(A) || pad(B))
        val aPadded = padTo(bigIntToBytes(A), nLen)
        val bPadded = padTo(bigIntToBytes(B), nLen)
        val u = BigInteger(1, sha512(aPadded, bPadded))

        // S = (B - k*g^x)^(a + u*x) mod N
        val gx = g.modPow(x, N)
        val kgx = k.multiply(gx).mod(N)
        var bMinusKgx = B.subtract(kgx).mod(N)
        if (bMinusKgx < BigInteger.ZERO) bMinusKgx = bMinusKgx.add(N)
        val exp = a.add(u.multiply(x))
        val S = bMinusKgx.modPow(exp, N)

        // M1 = H(H(N) XOR H(g) || H(username) || salt || A || B || H(S))
        val hN = sha512(nBytes)
        val hG = sha512(bigIntToBytes(g))
        val hNxorHG = ByteArray(hN.size) { i -> (hN[i].toInt() xor hG[i].toInt()).toByte() }
        val hUser = sha512(username.lowercase().toByteArray(Charsets.UTF_8))
        val hS = sha512(bigIntToBytes(S))

        val m1 = sha512(hNxorHG, hUser, rawSaltBytes, aPadded, bPadded, hS)

        // M2 = H(A || M1 || H(S))
        val m2 = sha512(aPadded, m1, hS)

        val clientEphemeral = Base64.encodeToString(bigIntToBytes(A), Base64.NO_WRAP)
        val clientProof = Base64.encodeToString(m1, Base64.NO_WRAP)
        val expectedServerProof = Base64.encodeToString(m2, Base64.NO_WRAP)

        return ProtonSrpProofs(
            clientEphemeral = clientEphemeral,
            clientProof = clientProof,
            expectedServerProof = expectedServerProof,
        )
    }
}

data class ProtonSrpProofs(
    /** Base64-encoded client ephemeral value A. */
    val clientEphemeral: String,
    /** Base64-encoded client proof M1, sent to server. */
    val clientProof: String,
    /** Base64-encoded expected server proof M2, to verify the server's response. */
    val expectedServerProof: String,
)
