package br.edu.eep.cademeusparca.location

import br.edu.eep.cademeusparca.model.LocalizacaoParticipante

object CameraMapaUtils {
    fun posicaoInicial(
        propriaAndroid: Pair<Double, Double>?,
        userIdAtual: String,
        localizacoes: List<LocalizacaoParticipante>,
        buscandoPosicaoPropria: Boolean
    ): Pair<Double, Double>? {
        if (userIdAtual.isBlank()) return null
        propriaAndroid?.takeIf(::utilParaCamera)?.let { return it }
        localizacoes.firstOrNull { it.userId == userIdAtual }?.let {
            (it.latitude to it.longitude).takeIf(::utilParaCamera)?.let { posicao -> return posicao }
        }
        // Enquanto a busca própria está em andamento, preserva sua prioridade.
        if (buscandoPosicaoPropria) return null
        return localizacoes.sortedBy { it.userId }
            .map { it.latitude to it.longitude }
            .firstOrNull(::utilParaCamera)
    }

    private fun utilParaCamera(posicao: Pair<Double, Double>): Boolean =
        DistanciaUtils.coordenadasValidas(posicao.first, posicao.second) &&
            posicao != (0.0 to 0.0)
}
