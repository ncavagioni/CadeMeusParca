package br.edu.eep.cademeusparca.viewmodel

import br.edu.eep.cademeusparca.location.DistanciaUtils
import br.edu.eep.cademeusparca.location.LocalizacaoTempoUtils
import br.edu.eep.cademeusparca.model.LocalizacaoParticipante

data class EstadoLocalizacaoParca(
    val textoPrincipal: String = "Localização indisponível",
    val recente: Boolean = false,
    val distancia: String? = null,
    val destino: LocalizacaoParticipante? = null
)

fun parametrosDetalhesValidos(roleId: String, userId: String, uidAtual: String?): Boolean =
    !uidAtual.isNullOrBlank() && userId != uidAtual &&
        listOf(roleId, userId).all { it.isNotBlank() && '/' !in it && it != "." && it != ".." }

fun estadoLocalizacaoParca(
    participante: LocalizacaoParticipante?,
    propriaAndroid: LocalizacaoParticipante?,
    propriaSalva: LocalizacaoParticipante?,
    agoraMs: Long
): EstadoLocalizacaoParca {
    val destino = participante?.takeIf {
        DistanciaUtils.coordenadasValidas(it.latitude, it.longitude)
    } ?: return EstadoLocalizacaoParca()
    val recente = LocalizacaoTempoUtils.estaAtualizada(destino.atualizadoEm, agoraMs)
    if (!recente) {
        return EstadoLocalizacaoParca(
            textoPrincipal = LocalizacaoTempoUtils.formatarVistoPorUltimo(
                destino.atualizadoEm, agoraMs
            ) ?: "Horário indisponível",
            destino = destino
        )
    }

    val origem = listOfNotNull(propriaAndroid, propriaSalva).firstOrNull {
        DistanciaUtils.coordenadasValidas(it.latitude, it.longitude)
    }
    val distancia = origem?.let {
        DistanciaUtils.calcularMetros(it.latitude, it.longitude, destino.latitude, destino.longitude)
            ?.let(DistanciaUtils::formatar)
    }
    return EstadoLocalizacaoParca(
        textoPrincipal = distancia?.let { "$it de você" } ?: "Localização recente",
        recente = true,
        distancia = distancia,
        destino = destino
    )
}
