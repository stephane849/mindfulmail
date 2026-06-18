package ca.lght.mindfulmail.domain.model

data class EmailAddress(val name: String?, val address: String) {
    override fun toString(): String = if (name != null) "$name <$address>" else address
}
