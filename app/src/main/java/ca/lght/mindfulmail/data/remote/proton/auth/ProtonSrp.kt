package ca.lght.mindfulmail.data.remote.proton.auth

import android.util.Base64
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import org.mindrot.jbcrypt.BCrypt

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

    private val g: BigInteger = BigInteger.valueOf(2)

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
    private fun computeK(N: BigInteger): BigInteger {
        val nBytes = bigIntToBytes(N)
        val gPadded = padTo(bigIntToBytes(g), nBytes.size)
        return BigInteger(1, sha512(nBytes, gPadded))
    }

    /**
     * BCrypt's custom base64 alphabet — same bit layout as standard base64 but different
     * character table, no padding. Used to derive the bcrypt salt from Proton's server salt.
     */
    private fun bcryptBase64Encode(data: ByteArray): String {
        val table = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder()
        var i = 0
        while (i < data.size) {
            val c1 = data[i].toInt() and 0xFF
            sb.append(table[c1 ushr 2])
            var carry = (c1 and 0x03) shl 4
            if (i + 1 >= data.size) { sb.append(table[carry]); break }
            val c2 = data[i + 1].toInt() and 0xFF
            carry = carry or (c2 ushr 4)
            sb.append(table[carry])
            carry = (c2 and 0x0f) shl 2
            if (i + 2 >= data.size) { sb.append(table[carry]); break }
            val c3 = data[i + 2].toInt() and 0xFF
            sb.append(table[carry or (c3 ushr 6)])
            sb.append(table[c3 and 0x3f])
            i += 3
        }
        return sb.toString()
    }

    /**
     * Hash the user's password according to Proton's auth version scheme.
     *
     * Versions 3 & 4 (current): BCrypt the password with a salt derived from the
     * Proton server salt using BCrypt's custom base64 alphabet.
     * Version 0 (legacy): plain SHA-512 of the password.
     */
    private fun hashPassword(password: String, salt: ByteArray, version: Int): ByteArray =
        when (version) {
            0 -> sha512(password.toByteArray(Charsets.UTF_8))
            3, 4 -> {
                // Proton's SRP salt is 10 bytes; bcrypt needs 16 bytes to produce 22
                // bcrypt-base64 chars. Pad to 16 with zero bytes (copyOf zero-fills).
                val saltPadded = if (salt.size < 16) salt.copyOf(16) else salt
                val bcryptSalt = "\$2a\$10\$${bcryptBase64Encode(saltPadded).take(22)}"
                BCrypt.hashpw(password, bcryptSalt).toByteArray(Charsets.UTF_8)
            }
            else -> throw IllegalArgumentException("Unsupported SRP auth version: $version")
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
        val saltBytes = Base64.decode(salt.trim(), Base64.DEFAULT)
        // Use the server's modulus (Proton issues a custom one per account, not RFC 5054)
        val N = BigInteger(1, Base64.decode(modulus.trim(), Base64.DEFAULT))
        val B = BigInteger(1, Base64.decode(serverEphemeral.trim(), Base64.DEFAULT))
        val k = computeK(N)

        val nBytes = bigIntToBytes(N)
        val nLen = nBytes.size

        val passwordHash = hashPassword(password, saltBytes, version)

        // x = H(salt || passwordHash)  — Proton's variant
        val x = BigInteger(1, sha512(saltBytes, passwordHash))

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

        val m1 = sha512(hNxorHG, hUser, saltBytes, aPadded, bPadded, hS)

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
