# Mapa multiusuário — recomendação de Security Rules

Esta recomendação não foi publicada. O projeto não contém uma cópia das regras atuais.
Para entregar um arquivo completo que preserve usuarios, roles, participantes e a consulta
collectionGroup("participantes"), é necessário primeiro obter o conteúdo publicado.

## Modelagem mínima para preservar os dados privados

- usuarios/{uid} continua contendo userId, parcaname, telefone e contatoEmergencia.
- Somente o próprio perfil privado é consultado pelo app nesta implementação.
- roles/{roleId}/localizacoes/{uid} mantém exatamente seus campos anteriores.
- Nova projeção: roles/{roleId}/perfisPublicos/{uid}, com apenas userId e parcaname.
- O próprio usuário publica seu Parcaname ao abrir o mapa. Isso não grava localização.
- Todos os usuários precisam abrir a nova versão uma vez para publicar o próprio nome.
- Os nomes têm limite de 100 caracteres na projeção; o cadastro privado não foi modificado.

O Firestore autoriza a leitura de documentos completos, não somente de um campo.
Por isso, não se recomenda abrir usuarios/{uid} aos demais participantes.

## Bloco completo da nova coleção

Adicionar dentro do match /databases/{database}/documents.
Preservar os blocos existentes, inclusive o de localizacoes, cuja leitura entre participantes
já está permitida segundo o contexto fornecido pelo usuário.

```javascript
match /roles/{roleId}/perfisPublicos/{userId} {
  function participanteDoRole() {
    return request.auth != null
      && exists(
        /databases/$(database)/documents/roles/$(roleId)/participantes/$(request.auth.uid)
      );
  }

  function perfilPublicoValido() {
    let dados = request.resource.data;
    return dados.keys().hasAll(['userId', 'parcaname'])
      && dados.keys().hasOnly(['userId', 'parcaname'])
      && dados.userId is string
      && dados.userId == request.auth.uid
      && dados.parcaname is string
      && dados.parcaname.size() > 0
      && dados.parcaname.size() <= 100
      && exists(
        /databases/$(database)/documents/usuarios/$(request.auth.uid)
      )
      && dados.parcaname == get(
        /databases/$(database)/documents/usuarios/$(request.auth.uid)
      ).data.parcaname;
  }

  allow read: if participanteDoRole();

  allow create, update: if participanteDoRole()
    && request.auth.uid == userId
    && perfilPublicoValido();

  allow delete: if false;
}
```

## Revisão lógica do bloco proposto

- Sem autenticação ou participação: leitura e escrita são negadas.
- Escrita no documento de outro UID: negada.
- Campo userId diferente do UID autenticado: negado.
- Nome diferente do próprio perfil privado: negado.
- Inclusão de telefone, contatoEmergencia ou campos extras: negada.
- Ausência de campos, tipos inválidos ou nome acima do limite: negados em create e update.
- Exclusão: negada.
- Leitura da coleção pelo participante: permitida; não depende do UID de cada resultado.
- Uma localização válida continua aparecendo mesmo sem perfil público; o título fica
  "Parça (nome indisponível)" até o nome ser publicado/carregado.
- Nenhuma nova consulta exige índice composto.

Avaliação estática limitada ao bloco acima, assumindo ausência de regras abrangentes que
autorizem os mesmos caminhos:

```json
{
  "score": 5,
  "summary": "O bloco proposto restringe leitura a participantes e publicação à própria identidade, com esquema fechado e nome conferido no perfil privado. A avaliação é lógica, não uma garantia sobre as regras publicadas.",
  "findings": []
}
```

Não foi executada validação de sintaxe no emulador de regras, nem teste de autorização no
Firebase real. A integração deve revisar todos os match existentes, pois permissões em
blocos sobrepostos são combinadas por OR. Estas são regras propostas para revisão antes
do uso amplo do aplicativo.

Referências:
- https://firebase.google.com/docs/firestore/security/rules-fields
- https://firebase.google.com/docs/firestore/security/rules-structure
