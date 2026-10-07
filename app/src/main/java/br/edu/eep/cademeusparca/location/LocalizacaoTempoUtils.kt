package br.edu.eep.cademeusparca.location

import com.google.firebase.Timestamp

object LocalizacaoTempoUtils {
    const val LIMITE_LOCALIZACAO_RECENTE_MS = 60_000L
    private const val MINUTO_MS = 60_000L
    private const val HORA_MS = 60 * MINUTO_MS
    private const val DIA_MS = 24 * HORA_MS

    fun idadeMs(atualizadoEm: Timestamp?, agoraMs: Long): Long? {
        atualizadoEm ?: return null
        val atualizadoEmMs = atualizadoEm.seconds * 1_000L +
            atualizadoEm.nanoseconds / 1_000_000L
        return (agoraMs - atualizadoEmMs).coerceAtLeast(0L)
    }

    fun estaAtualizada(atualizadoEm: Timestamp?, agoraMs: Long): Boolean {
        val idade = idadeMs(atualizadoEm, agoraMs) ?: return false
        return idade <= LIMITE_LOCALIZACAO_RECENTE_MS
    }

    fun formatarVistoPorUltimo(atualizadoEm: Timestamp?, agoraMs: Long): String? {
        val idade = idadeMs(atualizadoEm, agoraMs) ?: return null
        return when {
            idade < MINUTO_MS -> "Agora"
            idade < HORA_MS -> "Visto há ${idade / MINUTO_MS} min"
            idade < DIA_MS -> "Visto há ${idade / HORA_MS} h"
            idade < 2 * DIA_MS -> "Visto há 1 dia"
            else -> "Visto há ${idade / DIA_MS} dias"
        }
    }
}
