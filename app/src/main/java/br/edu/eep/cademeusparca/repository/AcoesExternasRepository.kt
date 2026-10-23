package br.edu.eep.cademeusparca.repository

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import br.edu.eep.cademeusparca.util.RotasExternasUtils
import br.edu.eep.cademeusparca.util.TelefoneUtils

class AcoesExternasRepository {
    fun ligar(context: Context, telefone: String): String? {
        val uri = TelefoneUtils.uriDiscador(telefone) ?: return "Telefone inválido ou não informado."
        return abrir(context, Intent(Intent.ACTION_DIAL, Uri.parse(uri)), "discador")
    }

    fun whatsapp(context: Context, telefone: String): String? {
        val uri = TelefoneUtils.uriWhatsApp(telefone) ?: return "Telefone inválido ou não informado."
        return abrir(context, Intent(Intent.ACTION_VIEW, Uri.parse(uri)), "WhatsApp ou navegador")
    }

    fun tracarRota(context: Context, latitude: Double, longitude: Double): String? {
        val uri = RotasExternasUtils.uriMaps(latitude, longitude)
            ?: return "Localização indisponível."
        try {
            // Abre a prévia de direções, com transporte escolhido pelo usuário.
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(uri)).setPackage("com.google.android.apps.maps")
            )
            return null
        } catch (_: ActivityNotFoundException) {
            return abrir(context, Intent(Intent.ACTION_VIEW, Uri.parse(uri)), "mapas ou navegador")
        } catch (_: SecurityException) {
            return "O Android não permitiu abrir o aplicativo de mapas."
        }
    }

    fun formatarMensagemCompartilhamento(nomeRole: String, codigo: String): String {
        val nomeTratado = nomeRole.trim().ifBlank { "Rolê" }
        return "Entra aí no meu rolê \"$nomeTratado\" no app Cadê meus parça! Use o código: $codigo"
    }

    fun compartilharCodigo(context: Context, nomeRole: String, codigo: String): String? {
        if (codigo.isBlank()) return "Código indisponível para compartilhamento."
        val mensagem = formatarMensagemCompartilhamento(nomeRole, codigo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, mensagem)
        }
        val chooser = Intent.createChooser(intent, "Compartilhar código do rolê")
        return abrir(context, chooser, "compartilhamento")
    }

    private fun abrir(context: Context, intent: Intent, aplicativo: String): String? {
        // Tentar abrir diretamente evita depender da visibilidade de pacotes do Android.
        return try {
            context.startActivity(intent)
            null
        } catch (_: ActivityNotFoundException) {
            "Nenhum aplicativo compatível disponível para abrir $aplicativo."
        } catch (_: SecurityException) {
            "O Android não permitiu abrir $aplicativo."
        }
    }
}
