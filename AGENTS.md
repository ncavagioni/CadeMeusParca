# Cadê meus parça? — PIC IV

## Objetivo

Aplicativo Android desenvolvido para a disciplina PIC IV.

O aplicativo permite criar grupos ("rolês"), entrar em grupos através de código e compartilhar a localização dos participantes.

Desenvolver somente o escopo solicitado. Evitar complexidade desnecessária.

## Tecnologias obrigatórias / definidas

- Kotlin
- Android Studio
- Jetpack Compose
- Material 3
- MVVM
- Firebase Authentication anônimo
- Cloud Firestore
- Firebase Storage quando necessário
- Google Maps SDK for Android
- Fused Location Provider
- Foreground Service para localização em segundo plano
- Android Intents
- Git/GitHub

Package:

br.edu.eep.cademeusparca

Min SDK:

26

## Arquitetura

Manter:

Compose Screen
→ ViewModel
→ Repository
→ Firebase / Android APIs

Estrutura principal:

br.edu.eep.cademeusparca
├── model
├── navigation
├── repository
├── service
├── ui
│   ├── components
│   ├── screens
│   └── theme
├── viewmodel
└── MainActivity.kt

Não concentrar regras de negócio em Composables.

## Identidade visual

Material 3.

Cores:

Primary: #6750A4
PrimaryContainer: #EADDFF
Secondary: #2E7D5B
Error: #BA1A1A
Background: #F9F9FC
Surface: #FFFFFF
TextPrimary: #1D1B20
TextSecondary: #625F66

Não utilizar Dynamic Color.

## Regras do usuário

Não existe login com e-mail e senha.

Utilizar Firebase Anonymous Authentication.

Perfil:

- userId: obrigatório
- parcaname: obrigatório
- telefone: opcional
- contatoEmergencia: opcional

Parcaname não precisa ser único.

## Rolês

Um usuário pode participar de vários rolês.

Somente o rolê atualmente aberto/ativo compartilha localização.

Ao ativar outro rolê, a localização deixa de ser compartilhada no anterior, mas o usuário continua sendo membro.

Rolê:

- nome obrigatório
- foto opcional
- local do evento opcional
- código alfanumérico de exatamente 6 caracteres
- criador é administrador

Qualquer usuário com código válido pode entrar sem aprovação do administrador.

## Localização

Utilizar GPS através do Fused Location Provider.

A localização deverá funcionar em segundo plano utilizando Foreground Service.

Comunicação deve funcionar por Wi-Fi e rede celular.

Não inventar posição quando não houver localização válida.

Quando a localização estiver antiga, mostrar "Visto por último".

Calcular distância aproximada em linha reta entre participantes.

## Mapa e rotas

Google Maps SDK é usado para exibir mapa e participantes.

O aplicativo NÃO calcula rota viária nem ETA internamente.

Ao escolher "Traçar rota", abrir Google Maps ou Waze através de Android Intent.

## Telefone e WhatsApp

Telefone deve abrir o discador.

Não realizar chamada automaticamente.

WhatsApp deve ser aberto através de Intent compatível.

## Telas principais

1. Splash
2. Cadastro inicial
3. Meus rolês
4. Criar um rolê
5. Rolê criado
6. Entrar em um rolê
7. Confirmar rolê
8. Mapa do rolê
9. Detalhes do parça
10. Traçar rota
11. Parças do rolê
12. Detalhes do rolê

Telas auxiliares podem ser criadas quando tecnicamente necessárias.

## Fluxo inicial

Ao iniciar:

1. Garantir Firebase Anonymous Authentication.
2. Consultar usuarios/{uid}.
3. Se o perfil não existir → Cadastro inicial.
4. Se o perfil existir → Meus rolês.

## Firestore

Coleção inicial:

usuarios/{uid}

Campos:

- userId
- parcaname
- telefone
- contatoEmergencia

Sempre respeitar Firebase Authentication e regras de segurança.

Não colocar banco de dados publicamente aberto em modo de teste.

## Desenvolvimento

Antes de alterar arquivos:

1. analisar a estrutura existente;
2. preservar código já funcionando;
3. não substituir arquitetura sem necessidade;
4. evitar adicionar bibliotecas desnecessárias.

Após alterações:

1. verificar imports;
2. executar build;
3. corrigir erros de compilação;
4. não alterar versões de SDK ou dependências sem necessidade.

Não apagar funcionalidades funcionando apenas para reconstruí-las de outra forma.

## Git

Trabalhar incrementalmente.

Não executar commit ou push sem autorização do usuário.

Ao terminar uma etapa, informar quais arquivos foram alterados.

## Testes

O projeto precisa gerar evidências para relatório acadêmico.

Quando uma funcionalidade importante estiver concluída, informar claramente:

"PRINT PARA O RELATÓRIO"

e indicar exatamente qual tela ou evidência deve ser capturada.

Testes finais devem incluir:

- autenticação anônima
- criação de perfil
- criação de rolê
- entrada por código
- dois usuários no mesmo rolê
- GPS
- Wi-Fi
- rede celular
- mapa
- atualização de localização
- segundo plano
- tela bloqueada
- distância entre usuários
- visto por último
- telefone
- WhatsApp
- rota externa
- código inválido
- perda e retorno da internet

## Regra principal

Faça somente a tarefa solicitada no prompt atual.

Não implemente antecipadamente outras etapas do projeto.

Explique ao final:

- arquivos alterados;
- o que foi implementado;
- como testar;
- possíveis problemas encontrados.

Não faça commit nem push sem confirmação.