package br.edu.eep.cademeusparca.viewmodel

/**
 * Uma escrita em andamento e somente a posição mais recente aguardando.
 * Usada exclusivamente na thread principal. Pausar não cancela uma Task do Firestore.
 */
internal class FilaUltimaPosicao<T> {
    private var emAndamento = false
    private var pendente: T? = null

    fun oferecer(posicao: T) {
        pendente = posicao
    }

    fun retirarParaGravar(): T? {
        if (emAndamento) return null
        val posicao = pendente ?: return null
        pendente = null
        emAndamento = true
        return posicao
    }

    fun concluir() {
        emAndamento = false
    }

    fun reporSeVazia(posicao: T) {
        if (pendente == null) pendente = posicao
    }

    fun descartarPendente() {
        pendente = null
    }
}
