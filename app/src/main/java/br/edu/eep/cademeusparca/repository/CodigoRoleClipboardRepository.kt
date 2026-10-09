package br.edu.eep.cademeusparca.repository

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class CodigoRoleClipboardRepository {
    fun copiar(context: Context, codigo: String): Boolean {
        if (codigo.isBlank()) return false
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        return try {
            clipboard.setPrimaryClip(ClipData.newPlainText("Código do rolê", codigo))
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
