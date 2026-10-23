# Cadê meus parça? — PIC IV

Aplicativo Android desenvolvido para a disciplina PIC IV. Permite criar grupos de encontros, chamados de **rolês**, e acompanhar a localização dos participantes para facilitar que se encontrem durante um evento.

## Integrantes

- Nicolas Camargo Cavagioni — RA 202010376
- Danilo Stefanovitz Costa — RA 202310083

## Tecnologias

Kotlin, Android Studio, Jetpack Compose, Material 3, Firebase Authentication anônimo, Cloud Firestore, Google Maps SDK for Android com Maps Compose, Fused Location Provider, Foreground Service, Android Intents, JUnit, Git e GitHub.

Arquitetura MVVM: **Screen → ViewModel → Repository → Firebase / APIs Android**.

Package: `br.edu.eep.cademeusparca`.

## Funcionalidades implementadas

- Autenticação anônima automática, cadastro e edição de perfil; Parcaname obrigatório e telefone e contato de emergência opcionais.
- Criação de rolês, entrada por código alfanumérico de seis caracteres, cópia e compartilhamento de convites.
- Participação em vários grupos, compartilhando localização somente no rolê ativo.
- Mapa e lista de parças, distância aproximada em linha reta e indicação “Visto por último” para posições antigas.
- Seleção opcional do local do evento no mapa, com coordenadas persistidas e marcador compacto próprio. Local e endereço também podem ser preenchidos manualmente.
- Edição de rolê ativo pelo administrador, preservando identidade, código e participantes; remoção de participantes, encerramento e exclusão de rolê encerrado.
- Abertura do discador, WhatsApp e rotas no Google Maps ou Waze. Não são realizadas chamadas automáticas nem cálculo interno de rota viária ou ETA.

## Requisitos e execução

- Android Studio compatível com **Android Gradle Plugin 9.4.1**.
- **JDK 25** para o daemon Gradle e **Android SDK Platform 37**.
- Aparelho Android 8.0 / API 26 ou superior, ou emulador com Google Play Services.
- Internet e localização disponíveis para validar mapas, sincronização e GPS.
- Configurações de Firebase e Google Maps descritas abaixo.

O Gradle Wrapper versionado usa **Gradle 9.6.0**; não é necessário instalar Gradle globalmente. Para usar o terminal, `JAVA_HOME` deve apontar para um JDK válido.

Abra a raiz do projeto no Android Studio, aguarde a sincronização, instale os componentes de SDK solicitados, selecione um dispositivo e execute o módulo `app`. Conceda as permissões necessárias no aplicativo. Selecionar manualmente um ponto de evento não exige nova permissão de localização.

O arquivo `local.properties`, gerado pela IDE com o caminho do SDK da máquina, não deve ser versionado. Mantenha as versões aprovadas dos arquivos Gradle.

## Google Maps

1. No projeto Google Cloud utilizado, habilite o **Maps SDK for Android** e configure o faturamento exigido pela plataforma.
2. Autorize a chave para o package `br.edu.eep.cademeusparca` e o SHA-1 do certificado que assina o aplicativo.
3. Copie `secrets.properties.example` para `secrets.properties`, na raiz, e preencha:

```properties
MAPS_API_KEY=SUA_CHAVE_DO_MAPS_SDK_FOR_ANDROID
```

Não versione esse arquivo nem publique a chave. Outro computador pode usar um certificado de depuração diferente. Consulte os certificados com:

```powershell
.\gradlew.bat signingReport
```

Sem chave configurada, o aplicativo informa a indisponibilidade do mapa. Endereços não são preenchidos automaticamente.

## Firebase Authentication e Firestore

`app/google-services.json` contém a configuração cliente do Firebase associado ao aplicativo. Para executar com outro projeto Firebase, cadastre nele um aplicativo Android com o mesmo package e obtenha o arquivo correspondente.

No Firebase Console do projeto utilizado:

1. Em **Authentication → Sign-in method**, habilite **Anonymous / Anônimo**.
2. Disponibilize o banco Cloud Firestore utilizado pelo aplicativo.
3. Reproduza as **Security Rules e os índices efetivamente aprovados no ambiente original**.

Não há login com e-mail e senha. O perfil é associado ao UID anônimo em `usuarios/{uid}`; rolês e suas subcoleções dependem das permissões do Firestore.

As regras e os índices **precisam ser reproduzidos no Firebase usado para executar o projeto**. O repositório não contém exportações completas dessas configurações. Os documentos em `docs/` são referências e propostas parciais, não substitutos das regras publicadas.

A consulta `collectionGroup("participantes").whereEqualTo("userId", uid)` precisa de índice compatível com o escopo de grupo de coleções. Obtenha a definição validada no ambiente original e reproduza-a. Este README não inventa regras nem uma configuração de índice.

Não use regras públicas em modo de teste. A configuração cliente não substitui autenticação nem Security Rules. Compilar o projeto não cria nem publica configurações remotas.

## Compilação e validação

No Windows, execute na raiz:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
git diff --check
```

No Linux/macOS, use `bash ./gradlew` no lugar de `.\gradlew.bat`.

| Verificação da versão aprovada | Resultado |
|---|---|
| Compilação de depuração | Aprovada |
| Testes unitários | **214 aprovados**, sem falhas |
| Verificações funcionais reais realizadas pelos integrantes | **80 aprovadas** |
| Lint | Concluído sem erros; avisos existentes mantidos |

Relatórios locais: `app/build/reports/tests/testDebugUnitTest/index.html` e `app/build/reports/lint-results-debug.html`. Os testes unitários não substituem a validação real de GPS, permissões, conectividade e segundo plano.

APK de depuração: `app/build/outputs/apk/debug/app-debug.apk`. O APK pode ser entregue separadamente; segredos, caminhos locais, caches, logs e saídas de compilação ficam fora do Git.
