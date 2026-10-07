# Tela 09 — perfil público do rolê

Nenhum arquivo local de Security Rules existia no projeto ao iniciar esta etapa.
Este documento é uma proposta de bloco para publicação MANUAL no Firebase Console.
Não foi feito deploy nem foi alterada a configuração remota.

## Publicação

Dentro do match existente de roles/{roleId}, substituir apenas o match de
perfisPublicos abaixo. Preservar os demais blocos, incluindo usuarios,
participantes e localizacoes. Não acrescentar este bloco em paralelo a uma
permissão antiga mais ampla, pois permissões Firestore são combinadas por OR.

O aplicativo passa a publicar sempre userId, parcaname e telefone (string,
inclusive vazia). Nunca publica contatoEmergencia. Cada usuário deve abrir
novamente o mapa após publicar as regras para atualizar sua própria projeção.
Projeções antigas sem telefone continuam legíveis no aplicativo como telefone vazio.

```javascript
match /perfisPublicos/{userId} {
  // Projeção: userId do proprietário, parcaname (1..100),
  // telefone (0..30), todos strings. Nenhum outro campo permitido.
  function leitorPerfilDoRole() {
    return request.auth != null
      && exists(/databases/$(database)/documents/roles/$(roleId))
      && exists(/databases/$(database)/documents/roles/$(roleId)/participantes/$(request.auth.uid))
      && get(/databases/$(database)/documents/roles/$(roleId)/participantes/$(request.auth.uid)).data.userId == request.auth.uid;
  }

  function perfilPublicoValido() {
    let dados = request.resource.data;
    let privado = get(/databases/$(database)/documents/usuarios/$(userId)).data;
    return dados.keys().hasAll(['userId', 'parcaname', 'telefone'])
      && dados.keys().hasOnly(['userId', 'parcaname', 'telefone'])
      && dados.userId is string
      && dados.userId.size() <= 128
      && dados.userId == request.auth.uid
      && dados.parcaname is string
      && dados.parcaname.size() > 0
      && dados.parcaname.size() <= 100
      && dados.parcaname == privado.parcaname
      && dados.telefone is string
      && dados.telefone.size() <= 30
      && dados.telefone == privado.get('telefone', '');
  }

  allow read: if leitorPerfilDoRole();
  allow create, update: if leitorPerfilDoRole()
    && request.auth.uid == userId
    && exists(/databases/$(database)/documents/usuarios/$(userId))
    && perfilPublicoValido();
  allow delete: if false;
}
```

## Escopo e pressupostos

- Projeto existente: cademeusparca-pic4; SDK Firestore Android já configurado.
- usuarios/{uid}: perfil privado do proprietário, com userId, parcaname,
  telefone opcional e contatoEmergencia opcional. A Tela 09 nunca consulta o
  perfil privado de outro usuário.
- roles/{roleId}/participantes/{uid}: identidade userId e papel. A Tela 09
  observa seu próprio documento e o do parça selecionado, com confirmação de
  participação pelo servidor. Pressupõe as permissões de leitura desses
  documentos para membros do rolê nas regras já existentes.
- roles/{roleId}/perfisPublicos/{uid}: leitura da coleção no mapa e observação
  do documento selecionado nos detalhes. A regra de read permite ambos os
  padrões apenas para membros do rolê.
- roles/{roleId}/localizacoes/{uid}: a Tela 09 reutiliza a observação existente
  da coleção para obter o destino e o próprio documento de fallback.
- Permanecem os fluxos existentes de consulta de roles por codigo com limit(1)
  e de collectionGroup(participantes) filtrado por userId. Este bloco não
  modifica suas regras nem seus índices.
- As regras remotas completas não estavam disponíveis localmente. Este bloco
  foi revisado estaticamente, mas não foi compilado/testado no Console ou no
  emulador de Firestore. As validações Gradle não validam Security Rules.
- Se as regras atuais de participantes não permitem a um membro ler o
  documento de outro membro do mesmo rolê, a verificação da Tela 09 retorna
  erro. Esse cenário precisa ser conferido no Console sem ampliar a leitura
  para pessoas fora do rolê.

## Revisão adversarial do bloco proposto

Análise estática; não representa testes executados contra o banco remoto.

1. Listar sem autenticação: negado pelo leitorPerfilDoRole.
2. Ler ou gravar fora do rolê: negado por participação e existência do rolê.
3. Criar válido e atualizar com payload inválido: mesmo validador em create/update.
4. Criar usando UID de outra pessoa: igualdade entre auth.uid, ID e campo userId.
5. Alterar proprietário: mesma igualdade em update impede reassociação.
6. Alterar campo imutável de outra coleção: fora do escopo; demais regras preservadas.
7. Trocar tipos de nome/telefone/UID: verificações is string.
8. Remover campo obrigatório por update: hasAll no estado final.
9. Strings enormes: nome <=100, telefone <=30, UID <=128.
10. Omitir campo em create: hasAll rejeita.
11. Autoconceder administrador: papel/isAdmin não estão nos campos permitidos.
12. Inserir campos extras ou contatoEmergencia: hasOnly rejeita.
13. Transição de status: não existe status nesta projeção.
14. Inserir referência para outro perfil: sem campos de caminhos; privado vem do UID autenticado.
15. Manipular timestamp: sem campo temporal nesta projeção.
16. Números negativos/overflow: tipos numéricos não são aceitos.
17. Ler usuarios de terceiros: nenhuma permissão nova para usuarios; projeção separada.
18. Repetir contador: não existem contadores nesta projeção.
19. Subcoleção de rolê inexistente: exists do documento do rolê.
20. Consulta do mapa versus regras: read usa participação do leitor, compatível com list/get.
21. Update apenas por propriedade: update exige propriedade E validador completo.
22. Publicar nome/telefone diferente do privado: igualdade com usuarios/{uid}.
23. Telefone privado ausente: comparação com string vazia por Map.get.

```json
{
  "score": 5,
  "summary": "Revisão estática do bloco proposto: proprietário, participação, esquema e igualdade com perfil privado verificados em create/update. Não avalia as regras remotas completas nem substitui validação no Console.",
  "findings": []
}
```

I've set up prototype Security Rules to keep the data in Firestore safe.
They are designed to be secure for role-member reads and owner-only writes
with exact schema, size checks, and equality to the private profile.
However, you should review and verify them before broadly sharing your app.
If you'd like, I can help you harden these rules.
