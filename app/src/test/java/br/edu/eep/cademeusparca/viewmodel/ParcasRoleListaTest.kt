package br.edu.eep.cademeusparca.viewmodel

import br.edu.eep.cademeusparca.model.LocalizacaoParticipante
import br.edu.eep.cademeusparca.model.ParcaListaUi
import br.edu.eep.cademeusparca.model.ParticipanteRole
import br.edu.eep.cademeusparca.model.PerfilPublicoRole
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParcasRoleListaTest {
    private val agora = 1_700_000_000_000L
    private val membros = listOf(ParticipanteRole("parca"), ParticipanteRole("eu"))
    private val perfis = listOf(PerfilPublicoRole("eu", "Zeca"), PerfilPublicoRole("parca", "Ana"))
    private val android = LocalizacaoParticipante("eu", -22.7, -47.6)
    private val destino = LocalizacaoParticipante(
        "parca", -22.701, -47.6, atualizadoEm = Timestamp(1_699_999_990L, 0)
    )

    private fun lista(
        participantes: List<ParticipanteRole> = membros,
        nomes: List<PerfilPublicoRole> = perfis,
        posicoes: List<LocalizacaoParticipante> = listOf(destino),
        origem: LocalizacaoParticipante? = android,
        tempo: Long = agora
    ): List<ParcaListaUi> = comporParcasDoRole(participantes, nomes, posicoes, "eu", origem, tempo)

    private fun outro(linhas: List<ParcaListaUi>) = linhas.single { it.userId == "parca" }

    @Test
    fun proprioUsuarioAparecePrimeiroMesmoComNomePosteriorNoAlfabeto() {
        assertEquals(listOf("eu", "parca"), lista().map { it.userId })
        assertTrue(lista().first().proprioUsuario)
    }

    @Test
    fun demaisParticipantesFicamEmOrdemAlfabeticaSemDependerDaDistancia() {
        val membros = listOf("c", "b", "a", "eu").map { ParticipanteRole(it) }
        val nomes = listOf(
            PerfilPublicoRole("c", "Carlos"), PerfilPublicoRole("b", "bia"),
            PerfilPublicoRole("a", "André"), PerfilPublicoRole("eu", "Zeca")
        )
        assertEquals(listOf("eu", "a", "b", "c"), lista(membros, nomes).map { it.userId })
        assertEquals(
            listOf("eu", "a", "b", "c"),
            lista(membros, nomes, listOf(destino.copy(userId = "c"))).map { it.userId }
        )
    }

    @Test
    fun participanteSemPerfilPermaneceComoParcaEApareceQuandoPerfilChega() {
        val semPerfil = lista(nomes = emptyList())
        assertEquals(2, semPerfil.size)
        assertEquals("Parça", outro(semPerfil).parcaname)
        assertEquals("Ana", outro(lista()).parcaname)
    }

    @Test
    fun participanteSemLocalizacaoMostraIndisponivel() {
        assertEquals("Localização indisponível", outro(lista(posicoes = emptyList())).informacaoLocalizacao)
    }

    @Test
    fun participanteRecenteMostraDistancia() {
        assertEquals("111 m", outro(lista()).informacaoLocalizacao)
    }

    @Test
    fun participanteAntigoMostraVistoHaSemDistancia() {
        assertEquals("Visto há 5 min", outro(lista(tempo = agora + 290_000L)).informacaoLocalizacao)
    }

    @Test
    fun proprioUsuarioNuncaMostraZeroMetrosNemVistoHa() {
        val posicoes = listOf(destino, android.copy(atualizadoEm = destino.atualizadoEm))
        for (tempo in listOf(agora, agora + 290_000L)) {
            assertEquals("Sua localização", lista(posicoes = posicoes, tempo = tempo).first().informacaoLocalizacao)
        }
    }

    @Test
    fun perfilOuLocalizacaoSemParticipanteNaoCriaMembroFantasma() {
        val linhas = lista(
            nomes = perfis + PerfilPublicoRole("fantasma", "Fantasma"),
            posicoes = listOf(destino, destino.copy(userId = "fantasma"))
        )
        assertEquals(listOf("eu", "parca"), linhas.map { it.userId })
    }

    @Test
    fun remocaoDeParticipanteRemoveItemMesmoMantendoPerfilEPosicaoAntigos() {
        assertEquals(2, lista().size)
        assertEquals(listOf("eu"), lista(participantes = listOf(ParticipanteRole("eu"))).map { it.userId })
    }

    @Test
    fun combinacaoNaoDuplicaUsuarios() {
        val linhas = lista(membros + membros, perfis + perfis, listOf(destino, destino))
        assertEquals(2, linhas.size)
        assertEquals(2, linhas.map { it.userId }.distinct().size)
    }

    @Test
    fun origemAndroidTemPrioridadeSobreDocumentoProprio() {
        val salva = destino.copy(userId = "eu")
        assertEquals("111 m", outro(lista(posicoes = listOf(destino, salva))).informacaoLocalizacao)
    }

    @Test
    fun origemSalvaEhFallbackSeAndroidAusenteInvalidoOuDeOutroUid() {
        val salva = destino.copy(userId = "eu")
        for (origem in listOf(null, android.copy(latitude = Double.NaN), android.copy(userId = "outro"))) {
            assertEquals("0 m", outro(lista(posicoes = listOf(destino, salva), origem = origem)).informacaoLocalizacao)
        }
    }

    @Test
    fun semOrigemNaoInventaDistancia() {
        assertEquals("Localização recente", outro(lista(origem = null)).informacaoLocalizacao)
    }

    @Test
    fun relogioMudaParaAntigaSemSnapshotENovaPosicaoRestauraDistancia() {
        assertEquals("111 m", outro(lista(tempo = agora + 50_000L)).informacaoLocalizacao)
        assertEquals("Visto há 1 min", outro(lista(tempo = agora + 50_001L)).informacaoLocalizacao)
        val atualizada = destino.copy(atualizadoEm = Timestamp(1_700_000_051L, 0))
        assertEquals("111 m", outro(lista(posicoes = listOf(atualizada), tempo = agora + 51_000L)).informacaoLocalizacao)
    }

    @Test
    fun nomesIguaisTemDesempatePrevisivelPorUid() {
        val membros = listOf(ParticipanteRole("b"), ParticipanteRole("a"))
        val nomes = listOf(PerfilPublicoRole("b", "Ana"), PerfilPublicoRole("a", "ana"))
        assertEquals(listOf("a", "b"), lista(membros, nomes).map { it.userId })
    }

    @Test
    fun apenasProprioUsuarioEhListaValidaEListaVaziaNaoInventaMembros() {
        assertEquals(1, lista(participantes = listOf(ParticipanteRole("eu"))).size)
        assertTrue(lista(participantes = emptyList()).isEmpty())
        assertTrue(lista(participantes = listOf(ParticipanteRole(""))).isEmpty())
    }
}
