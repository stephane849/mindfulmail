package ca.lght.mindfulmail.domain.model

data class Account(
    val id: String,
    val displayName: String,
    val email: String,
    val type: AccountType,
)

enum class AccountType { PROTON, IMAP }
