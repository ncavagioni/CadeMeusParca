package br.edu.eep.cademeusparca.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ParticipanteMapMarker(
    titulo: String,
    proprioUsuario: Boolean,
    distancia: String? = null,
    vistoPorUltimo: String? = null,
    localizacaoRecente: Boolean = true
) {
    val corMarcador = when {
        proprioUsuario -> MaterialTheme.colorScheme.primary
        !localizacaoRecente -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.secondary
    }
    val corInicial = when {
        proprioUsuario -> MaterialTheme.colorScheme.onPrimary
        !localizacaoRecente -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.onSecondary
    }
    val textoSecundario = if (localizacaoRecente) distancia else vistoPorUltimo
    val inicial = titulo.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 160.dp)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                textoSecundario?.takeUnless { proprioUsuario }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.size(3.dp))
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = corMarcador,
            contentColor = corInicial,
            shadowElevation = 3.dp,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = inicial,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = corMarcador,
            shadowElevation = 2.dp
        ) {}
    }
}
