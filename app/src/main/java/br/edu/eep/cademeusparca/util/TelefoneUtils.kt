package br.edu.eep.cademeusparca.util

object TelefoneUtils {
    fun somenteDigitos(telefone: String): String = telefone.filter { it in '0'..'9' }

    fun numeroWhatsApp(telefone: String): String? {
        val entrada = telefone.trim()
        if (entrada.isEmpty() || entrada.length > 30 ||
            entrada.any { it !in '0'..'9' && it !in " +()-." } ||
            entrada.count { it == '+' } > 1 ||
            ('+' in entrada && !entrada.startsWith('+'))
        ) return null

        val digitos = somenteDigitos(entrada)
        if (entrada.startsWith('+') && !digitos.startsWith("55")) return null
        val nacional = when {
            digitos.length in 10..11 && !entrada.startsWith('+') -> digitos
            digitos.length in 12..13 && digitos.startsWith("55") -> digitos.drop(2)
            else -> return null
        }
        // Não inventar DDD nem completar números incompletos.
        if (nacional[0] !in '1'..'9' || nacional[1] !in '1'..'9' ||
            (nacional.length == 11 && nacional[2] != '9') ||
            (nacional.length == 10 && nacional[2] !in '2'..'5')
        ) return null
        return "55$nacional"
    }

    fun uriWhatsApp(telefone: String): String? =
        numeroWhatsApp(telefone)?.let { "https://wa.me/$it" }

    fun uriDiscador(telefone: String): String? {
        numeroWhatsApp(telefone) ?: return null
        val numero = somenteDigitos(telefone)
        val temCodigoPais = numero.length in 12..13 && numero.startsWith("55")
        val prefixo = if (temCodigoPais) "+" else ""
        return "tel:$prefixo$numero"
    }
}
