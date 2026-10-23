package br.edu.eep.cademeusparca.viewmodel

import br.edu.eep.cademeusparca.model.EdicaoRole
import br.edu.eep.cademeusparca.model.PontoLocal
import br.edu.eep.cademeusparca.model.Role
import br.edu.eep.cademeusparca.repository.EdicaoRoleRepository
import org.junit.Assert.*
import org.junit.Test

class EditarRoleViewModelTest {
    private val role = Role(
        roleId = "role", nome = "Rolê original", nomeLocal = "Praça", endereco = "Rua A",
        latitudeLocal = -22.0, longitudeLocal = -47.0, adminId = "admin", codigo = "ABC123"
    )

    private class FakeRepository(val role: Role) : EdicaoRoleRepository {
        override var userIdAtual: String? = "admin"
        var leituras = 0
        var adiarLeitura = false
        var retornoLeitura: ((Role?, String?) -> Unit)? = null
        val gravacoes = mutableListOf<Pair<String, EdicaoRole>>()
        var retornoGravacao: ((Boolean, String?) -> Unit)? = null

        override fun buscarRolePorId(roleId: String, onResult: (Role?, String?) -> Unit) {
            leituras++
            if (adiarLeitura) retornoLeitura = onResult else onResult(role, null)
        }

        override fun atualizarDadosRole(
            roleId: String, edicao: EdicaoRole, onResult: (Boolean, String?) -> Unit
        ) {
            gravacoes += roleId to edicao
            retornoGravacao = onResult
        }

        fun concluirGravacao(sucesso: Boolean, erro: String? = null) {
            val retorno = checkNotNull(retornoGravacao)
            retornoGravacao = null
            retorno(sucesso, erro)
        }
    }

    private fun carregar(repository: FakeRepository = FakeRepository(role)) =
        EditarRoleViewModel(repository).also { it.carregarRole("role") }

    @Test
    fun adminCarregaValoresAtuaisEPodeSalvar() {
        val vm = carregar()
        assertTrue(vm.podeSalvar)
        assertEquals("Rolê original", vm.formulario?.nome)
        assertEquals("Praça", vm.formulario?.nomeLocal)
        assertEquals("Rua A", vm.formulario?.endereco)
        assertEquals(PontoLocal(-22.0, -47.0), vm.formulario?.pontoSelecionado)
    }

    @Test
    fun participanteNaoRecebeFormularioNemEnviaGravacao() {
        val repo = FakeRepository(role).apply { userIdAtual = "parca" }
        val vm = carregar(repo)
        vm.salvarAlteracoes()
        assertNull(vm.formulario)
        assertFalse(vm.podeSalvar)
        assertTrue(vm.mensagem.contains("administrador"))
        assertTrue(repo.gravacoes.isEmpty())
    }

    @Test
    fun roleEncerradoNaoRecebeFormularioNemEnviaGravacao() {
        val repo = FakeRepository(role.copy(status = "encerrado"))
        val vm = carregar(repo)
        vm.salvarAlteracoes()
        assertNull(vm.formulario)
        assertTrue(vm.mensagem.contains("ativos"))
        assertTrue(repo.gravacoes.isEmpty())
    }

    @Test
    fun roleAntigoSemCoordenadasPodeSerEditado() {
        val repo = FakeRepository(role.copy(latitudeLocal = null, longitudeLocal = null))
        val vm = carregar(repo)
        assertNull(vm.formulario?.pontoSelecionado)
        vm.atualizarNome("Novo")
        vm.salvarAlteracoes()
        val edicao = repo.gravacoes.single().second
        assertEquals("Novo", edicao.nome)
        assertNull(edicao.latitudeLocal)
        assertNull(edicao.longitudeLocal)
    }

    @Test
    fun nomeVazioImpedeGravacao() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.atualizarNome("  ")
        vm.salvarAlteracoes()
        assertTrue(vm.nomeInvalido)
        assertTrue(repo.gravacoes.isEmpty())
        assertFalse(vm.salvando)
        vm.atualizarNome("Nome corrigido")
        assertFalse(vm.nomeInvalido)
    }

    @Test
    fun cancelarEdicaoNaoSalvaEIgnoraTentativaPosteriorDeSalvar() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.atualizarNome("Rascunho")
        assertTrue(vm.cancelarEdicao())
        vm.salvarAlteracoes()
        assertTrue(repo.gravacoes.isEmpty())
        assertEquals("Rolê original", repo.role.nome)
    }

    @Test
    fun cancelarMapaPreservaTextosEPontoOriginalSemGravar() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.atualizarNome("Nome digitado")
        vm.atualizarNomeLocal("Local digitado")
        vm.atualizarEndereco("Endereço digitado")
        val antes = vm.formulario
        vm.abrirSeletorLocal()
        vm.selecionarPonto(-23.0, -46.0)
        vm.cancelarSelecaoLocal()
        assertEquals(antes, vm.formulario)
        assertTrue(repo.gravacoes.isEmpty())
    }

    @Test
    fun confirmarMapaAplicaSomenteRascunhoSemSalvarNoServidor() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.atualizarNome("Nome digitado")
        vm.abrirSeletorLocal()
        vm.selecionarPonto(-23.0, -46.0)
        vm.confirmarLocal()
        assertEquals(PontoLocal(-23.0, -46.0), vm.formulario?.pontoSelecionado)
        assertEquals("Nome digitado", vm.formulario?.nome)
        assertFalse(vm.formulario!!.selecionandoLocal)
        assertTrue(repo.gravacoes.isEmpty())
    }

    @Test
    fun limparPontoEnviaAmbasCoordenadasNulas() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.limparLocal()
        vm.salvarAlteracoes()
        assertNull(repo.gravacoes.single().second.latitudeLocal)
        assertNull(repo.gravacoes.single().second.longitudeLocal)
    }

    @Test
    fun pontoInvalidoNaoSubstituiSelecaoAnterior() {
        val vm = carregar()
        vm.abrirSeletorLocal()
        vm.selecionarPonto(91.0, 0.0)
        vm.confirmarLocal()
        assertEquals(PontoLocal(-22.0, -47.0), vm.formulario?.pontoSelecionado)
    }

    @Test
    fun soConcluiAposConfirmacaoDoRepositoryENaoDuplicaEnvio() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.atualizarNome("Novo nome")
        vm.salvarAlteracoes()
        vm.salvarAlteracoes()
        assertTrue(vm.salvando)
        assertFalse(vm.salvo)
        assertFalse(vm.podeSalvar)
        assertEquals(1, repo.gravacoes.size)
        assertEquals("role", repo.gravacoes.single().first)
        repo.concluirGravacao(true)
        assertTrue(vm.salvo)
        assertFalse(vm.salvando)
        vm.salvarAlteracoes()
        assertEquals(1, repo.gravacoes.size)
    }

    @Test
    fun naoPermiteCancelarNemAlterarFormularioDuranteGravacao() {
        val vm = carregar()
        val antes = vm.formulario
        vm.salvarAlteracoes()
        assertFalse(vm.cancelarEdicao())
        vm.atualizarNome("Outro")
        assertEquals(antes, vm.formulario)
    }

    @Test
    fun falhaDeGravacaoMantemRascunhoEPermiteTentarNovamente() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.atualizarEndereco("Endereço novo")
        vm.salvarAlteracoes()
        repo.concluirGravacao(false, "Sem conexão")
        assertFalse(vm.salvo)
        assertFalse(vm.salvando)
        assertTrue(vm.podeSalvar)
        assertEquals("Sem conexão", vm.mensagem)
        assertEquals("Endereço novo", vm.formulario?.endereco)
        vm.salvarAlteracoes()
        assertEquals(2, repo.gravacoes.size)
    }

    @Test
    fun rejeicaoPorEncerramentoConcorrenteNaoConcluiEdicao() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.salvarAlteracoes()
        repo.concluirGravacao(false, "Somente rolês ativos podem ser editados.")
        assertFalse(vm.salvo)
        assertFalse(vm.salvando)
        assertTrue(vm.mensagem.contains("ativos"))
    }

    @Test
    fun recarregarAoRetornarDoMapaNaoSobrescreveTextosNemSelecao() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.atualizarNome("Rascunho")
        vm.abrirSeletorLocal()
        vm.selecionarPonto(-23.0, -46.0)
        vm.confirmarLocal()
        val antes = vm.formulario
        vm.carregarRole("role")
        assertEquals(antes, vm.formulario)
        assertEquals(1, repo.leituras)
    }

    @Test
    fun cancelarDuranteLeituraIgnoraRespostaAtrasada() {
        val repo = FakeRepository(role).apply { adiarLeitura = true }
        val vm = carregar(repo)
        assertTrue(vm.carregando)
        vm.cancelarEdicao()
        repo.retornoLeitura!!(role, null)
        assertFalse(vm.carregando)
        assertNull(vm.formulario)
        assertFalse(vm.podeSalvar)
    }

    @Test
    fun falhaNaLeituraExibeErroEPermiteRepetir() {
        val repo = FakeRepository(role).apply { adiarLeitura = true }
        val vm = carregar(repo)
        repo.retornoLeitura!!(null, "Sem internet")
        assertFalse(vm.carregando)
        assertEquals("Sem internet", vm.mensagem)
        assertNull(vm.formulario)
        repo.adiarLeitura = false
        vm.carregarRole("role")
        assertNotNull(vm.formulario)
        assertEquals(2, repo.leituras)
    }

    @Test
    fun trocaDeUsuarioAntesDeSalvarBloqueiaEnvio() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        repo.userIdAtual = "parca"
        vm.salvarAlteracoes()
        assertFalse(vm.podeSalvar)
        assertTrue(repo.gravacoes.isEmpty())
        assertFalse(vm.salvo)
    }

    @Test
    fun trocaDeUsuarioDuranteGravacaoNaoNavegaComoSucesso() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.salvarAlteracoes()
        repo.userIdAtual = "parca"
        repo.concluirGravacao(true)
        assertFalse(vm.salvo)
        assertTrue(vm.mensagem.contains("autenticação"))
    }

    @Test
    fun cancelarMapaAposTrocaDeUsuarioAindaFechaOSeletor() {
        val repo = FakeRepository(role)
        val vm = carregar(repo)
        vm.abrirSeletorLocal()
        repo.userIdAtual = "parca"
        vm.cancelarSelecaoLocal()
        assertFalse(vm.formulario!!.selecionandoLocal)
        assertTrue(repo.gravacoes.isEmpty())
    }

    @Test
    fun rotaInvalidaOuSemAutenticacaoNaoConsultaRepository() {
        val repo = FakeRepository(role)
        val vm = EditarRoleViewModel(repo)
        vm.carregarRole("outro/role")
        assertEquals(0, repo.leituras)
        repo.userIdAtual = null
        vm.carregarRole("role")
        assertEquals(0, repo.leituras)
        assertFalse(vm.podeSalvar)
    }

    @Test
    fun detalhesExibemEdicaoSomenteAoAdminDoRoleAtivo() {
        fun dados(evento: Role, uid: String) = comporDetalhesRole(evento, uid, emptyList(), 2)
        assertTrue(dados(role, "admin").podeEditar)
        assertFalse(dados(role, "parca").podeEditar)
        assertFalse(dados(role.copy(status = "encerrado"), "admin").podeEditar)
        assertFalse(dados(role.copy(status = "inativo"), "admin").podeEditar)
    }
}
