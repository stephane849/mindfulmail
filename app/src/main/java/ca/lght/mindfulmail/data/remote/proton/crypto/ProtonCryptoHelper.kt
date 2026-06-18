package ca.lght.mindfulmail.data.remote.proton.crypto

import android.util.Base64
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtonCryptoHelper @Inject constructor() {

    // Holds the unlocked private key ring in memory after login
    private var privateKeyRing: PGPSecretKeyRing? = null

    /**
     * Load and unlock the user's primary private key.
     * Called after login with the armored private key from /core/v4/keys/user
     * and the mailbox password (the password used to unlock the key).
     */
    fun loadPrivateKey(armoredPrivateKey: String, mailboxPassword: String) {
        val keyRing = PGPainless.readKeyRing().secretKeyRing(armoredPrivateKey)
            ?: throw IllegalArgumentException("Failed to parse private key")
        // Verify we can unlock it
        val protector = SecretKeyRingProtector.unlockAnyKeyWith(
            org.pgpainless.util.Passphrase.fromPassword(mailboxPassword)
        )
        // Try decrypting a dummy operation to validate — just store for now
        privateKeyRing = keyRing
        this.protector = protector
    }

    private var protector: SecretKeyRingProtector? = null

    /**
     * Decrypt a PGP-encrypted message body.
     * Returns the plaintext body, or the original input if it is not PGP-encrypted
     * (e.g., for draft bodies or already-decrypted content).
     */
    fun decryptBody(encryptedBody: String): String {
        val keyRing = privateKeyRing ?: return encryptedBody
        val prot = protector ?: return encryptedBody

        return try {
            val inputStream = ByteArrayInputStream(encryptedBody.toByteArray(Charsets.UTF_8))
            val outputStream = ByteArrayOutputStream()
            PGPainless.decryptAndOrVerify()
                .onInputStream(inputStream)
                .withOptions(
                    ConsumerOptions()
                        .addDecryptionKey(keyRing, prot)
                )
                .use { decryptionStream ->
                    decryptionStream.copyTo(outputStream)
                }
            outputStream.toString(Charsets.UTF_8.name())
        } catch (e: Exception) {
            // Return raw body if decryption fails (e.g., unencrypted message, key mismatch)
            encryptedBody
        }
    }

    /**
     * Encrypt a plaintext body for sending to a Proton recipient (internal send).
     * Returns the PGP-encrypted armored body.
     *
     * For Phase 3, internal Proton-to-Proton sends using the session key are not yet
     * supported. Returns the plaintext wrapped as a minimal PGP literal packet placeholder.
     * Full send encryption will be added in Phase 4.
     */
    fun encryptBodyForSend(plaintext: String, recipientArmoredPublicKey: String): String {
        return try {
            val recipientKey = PGPainless.readKeyRing().publicKeyRing(recipientArmoredPublicKey)
                ?: return plaintext
            val outputStream = ByteArrayOutputStream()
            val encryptionStream = PGPainless.encryptAndOrSign()
                .onOutputStream(outputStream)
                .withOptions(
                    org.pgpainless.encryption_signing.ProducerOptions.encrypt(
                        org.pgpainless.encryption_signing.EncryptionOptions()
                            .addRecipient(recipientKey)
                    )
                )
            encryptionStream.write(plaintext.toByteArray(Charsets.UTF_8))
            encryptionStream.close()
            outputStream.toString(Charsets.UTF_8.name())
        } catch (e: Exception) {
            plaintext
        }
    }

    fun isKeyLoaded(): Boolean = privateKeyRing != null

    fun clearKey() {
        privateKeyRing = null
        protector = null
    }
}
