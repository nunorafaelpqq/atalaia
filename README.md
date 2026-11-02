# 🌍 Atalaia

Clica num país no mapa e descobre-o: o que se passa lá hoje (notícias, o que as pessoas pesquisam, principais jornais), curiosidades e o que aconteceu neste dia na história.

> *Atalaia*: torre de vigia de onde se observa o horizonte.

> Protótipo (v0.1). Backend em **Java + Spring Boot**, frontend em **JavaScript + Leaflet**.

---

## 1. Instalar o que é preciso (só uma vez)

| O quê | Para quê | Como |
|---|---|---|
| **JDK 21** (Java) | correr o backend | [adoptium.net](https://adoptium.net) → Temurin 21 (LTS). No Windows marca "Set JAVA_HOME" no instalador. |
| **VS Code** | editar o código | já tens 👍 |
| **Extension Pack for Java** + **Spring Boot Extension Pack** | o VS Code perceber Java/Spring | ao abrires a pasta, o VS Code sugere-as (canto inferior direito) → *Install* |
| **Git** | guardar versões e enviar para o GitHub | [git-scm.com](https://git-scm.com) |
| *(opcional)* **Maven** | correr pelo terminal | Windows: `winget install Apache.Maven` · Mac: `brew install maven` |

Confirma no terminal: `java -version` deve dizer 21 (ou mais).

## 2. Correr o projeto

### Opção A — pelo VS Code (mais fácil)
1. `File → Open Folder…` → escolhe a pasta `atalaia`.
2. Espera que a barra de baixo diga que o projeto Java está pronto (a 1.ª vez demora uns minutos: está a descarregar as bibliotecas).
3. Abre `src/main/java/com/atalaia/AtalaiaApplication.java` e clica em **Run** por cima do `main`.
4. Abre **http://localhost:8080** no browser. 🎉

### Opção B — pelo terminal (precisa do Maven)
```bash
mvn spring-boot:run
```

Para parar: `Ctrl + C` no terminal (ou o quadrado vermelho no VS Code).

Dica: a API também se vê diretamente no browser → http://localhost:8080/api/country/PT

## 3. Como está organizado

```
atalaia/
├── pom.xml                         ← lista de dependências (tipo package.json do Java)
└── src/main/
    ├── java/com/atalaia/
    │   ├── AtalaiaApplication.java   ← arranque
    │   ├── web/CountryController.java   ← a API: GET /api/country/{código}
    │   ├── service/
    │   │   ├── CountryService.java      ← junta tudo, em paralelo
    │   │   ├── CountryInfoService.java  ← bandeira, capital, população (REST Countries)
    │   │   ├── NewsService.java         ← notícias (Google News RSS)
    │   │   ├── TrendsService.java       ← em alta (Google Trends RSS)
    │   │   ├── OnThisDayService.java    ← neste dia (Wikipedia)
    │   │   └── NewspaperService.java    ← jornais (lista feita à mão)
    │   ├── support/                     ← cache e leitor de RSS
    │   └── model/Models.java            ← formato dos dados
    └── resources/
        ├── application.properties       ← configurações
        ├── data/newspapers.json         ← ✏️ acrescenta jornais aqui
        └── static/                      ← o site (servido pelo Spring)
            ├── index.html
            ├── css/style.css
            ├── js/app.js                ← mapa + painel
            └── data/countries.geojson   ← fronteiras dos países
```

**Como funciona um clique:**
```
Browser (app.js)  ──GET /api/country/PT──►  CountryController
                                                 │
                                           CountryService  (pede tudo ao mesmo tempo)
                          ┌──────────┬───────────┼────────────┬────────────┐
                     REST Countries  Google News  Google Trends  Wikipedia  newspapers.json
                          └──────────┴───────────┴────────────┴────────────┘
                                                 │   (guardado em cache 20 min–24 h)
Browser  ◄────────────── JSON com tudo ─────────┘
```

Se uma fonte falhar, as outras continuam a aparecer — o painel mostra "Fonte indisponível" só nessa secção.

## 4. Pôr no GitHub

1. Cria conta em [github.com](https://github.com) e um repositório novo chamado `atalaia` (**sem** README, porque já tens um).
2. No terminal do VS Code (`Ctrl + '`), dentro da pasta do projeto:
   ```bash
   git init
   git add .
   git commit -m "Primeiro protótipo do Atalaia"
   git branch -M main
   git remote add origin https://github.com/nunorafaelpqq/atalaia.git
   git push -u origin main
   ```
3. A partir daí, sempre que fizeres alterações:
   ```bash
   git add .
   git commit -m "o que mudaste"
   git push
   ```
   (Também dá para fazer isto pelos botões do separador **Source Control** do VS Code.)

Depois de criares o repositório, troca o link em `application.properties` (`atalaia.user-agent`) pelo teu.

## 5. Limitações conhecidas

- **Twitter/X** não está incluído: a API é paga. O "Em alta" usa o Google Trends.
- **Google Trends** não tem dados para todos os países.
- **Notícias**: ~50 países têm edição local do Google News (ver `NewsService.EDITIONS`); os outros usam pesquisa global em inglês.
- **"Neste dia"** vem da Wikipedia em inglês e é filtrado pelo nome do país, por isso às vezes não há nada.
- O mapa é de baixa resolução: países muito pequenos (Malta, Singapura, Andorra…) não aparecem, mas a API funciona para eles.
- Os RSS do Google não são uma API oficial: são ótimos para um protótipo, mas para um produto a sério convém mudar para uma API de notícias (ex.: GDELT, NewsAPI).

## 6. Próximos passos (ideias)

- [ ] Mais jornais em `newspapers.json`
- [ ] Reddit (subreddit de cada país) e Bluesky para "o que se fala nas redes"
- [ ] Traduzir títulos para português (DeepL tem plano grátis)
- [ ] Mapa de calor: países com mais notícias hoje
- [ ] Comparar dois países lado a lado
- [ ] Publicar online (Render, Railway ou Fly.io)
