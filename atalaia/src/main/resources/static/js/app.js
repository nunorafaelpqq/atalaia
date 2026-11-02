/*
 * Atalaia — frontend
 * 1. Desenha o mapa com os países (ficheiro data/countries.geojson)
 * 2. Quando clicas num país, pede ao backend /api/country/{código}
 * 3. Mostra o resultado no painel da direita
 */

const STYLE = {
  normal:   { color: "#2e3c55", weight: 0.8, fillColor: "#1f2a3d", fillOpacity: 1 },
  hover:    { color: "#f2a93b", weight: 1.5, fillColor: "#3a3324", fillOpacity: 1 },
  selected: { color: "#f26b3b", weight: 2,   fillColor: "#5a2f22", fillOpacity: 1 },
  disabled: { color: "#1c2536", weight: 0.5, fillColor: "#161e2c", fillOpacity: 1 },
};

const panel = document.getElementById("panel");
const fmtNumber = new Intl.NumberFormat("pt-PT");
const fmtRelative = new Intl.RelativeTimeFormat("pt-PT", { numeric: "auto" });

let map;
let selectedLayer = null;
let requestCounter = 0;            // evita mostrar respostas antigas se clicares depressa
const layersByCode = new Map();    // "PT" -> camada do mapa
const featuresByName = new Map();  // "portugal" -> feature

// ---------------------------------------------------------------------------
// Mapa
// ---------------------------------------------------------------------------

async function initMap() {
  map = L.map("map", {
    worldCopyJump: true,
    minZoom: 2,
    maxZoom: 7,
    zoomSnap: 0.5,
    attributionControl: true,
  }).setView([30, 10], 2.5);

  map.attributionControl.setPrefix(false);
  map.attributionControl.addAttribution("Fronteiras: Natural Earth");

  const geojson = await fetch("data/countries.geojson").then((r) => r.json());

  L.geoJSON(geojson, {
    style: (f) => (f.properties.code ? STYLE.normal : STYLE.disabled),
    onEachFeature: (feature, layer) => {
      const { code, name } = feature.properties;
      if (!code) return; // territórios sem código ISO: não clicáveis

      layersByCode.set(code, layer);
      featuresByName.set(name.toLowerCase(), feature);

      layer.bindTooltip(name, { sticky: true, className: "country-tip", direction: "top", offset: [0, -8] });
      layer.on({
        mouseover: () => layer !== selectedLayer && layer.setStyle(STYLE.hover),
        mouseout: () => layer !== selectedLayer && layer.setStyle(STYLE.normal),
        click: () => selectCountry(code),
      });
    },
  }).addTo(map);

  fillSearchList(geojson.features);

  // Se o endereço tiver #PT, abre logo Portugal (dá para partilhar links!)
  const fromHash = location.hash.replace("#", "").toUpperCase();
  if (layersByCode.has(fromHash)) selectCountry(fromHash, { fly: true });
}

function fillSearchList(features) {
  const list = document.getElementById("country-list");
  features
    .filter((f) => f.properties.code)
    .map((f) => f.properties.name)
    .sort((a, b) => a.localeCompare(b, "pt"))
    .forEach((name) => list.append(el("option", { value: name })));

  const input = document.getElementById("search");
  input.addEventListener("change", () => {
    const feature = featuresByName.get(input.value.trim().toLowerCase());
    if (feature) {
      selectCountry(feature.properties.code, { fly: true });
      input.value = "";
      input.blur();
    }
  });
}

function selectCountry(code, { fly = false } = {}) {
  const layer = layersByCode.get(code);
  if (!layer) return;

  if (selectedLayer) selectedLayer.setStyle(STYLE.normal);
  selectedLayer = layer;
  layer.setStyle(STYLE.selected);
  layer.bringToFront();
  if (fly) map.flyToBounds(layer.getBounds(), { maxZoom: 5, duration: 0.8 });

  history.replaceState(null, "", "#" + code);
  loadCountry(code, layer.feature.properties);
}

// ---------------------------------------------------------------------------
// Dados
// ---------------------------------------------------------------------------

async function loadCountry(code, props) {
  const myRequest = ++requestCounter;
  renderLoading(props);

  try {
    const url = `/api/country/${code}?name=${encodeURIComponent(props.nameEn || props.name)}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error("HTTP " + res.status);
    const data = await res.json();
    if (myRequest !== requestCounter) return; // entretanto clicaste noutro país
    renderCountry(data, props);
  } catch (err) {
    if (myRequest !== requestCounter) return;
    console.error(err);
    panel.replaceChildren(
      header(props, null),
      el("p", { className: "error" }, "Não foi possível falar com o servidor. Está a correr? (" + err.message + ")")
    );
  }
}

// ---------------------------------------------------------------------------
// Desenhar o painel
// ---------------------------------------------------------------------------

function renderLoading(props) {
  const skeletonSection = (title) =>
    el("section", { className: "section" },
      el("h2", {}, title),
      el("div", { className: "skeleton", style: "width:90%" }),
      el("div", { className: "skeleton", style: "width:75%" }),
      el("div", { className: "skeleton", style: "width:82%" }));

  panel.replaceChildren(
    header(props, null),
    skeletonSection("📰 Notícias"),
    skeletonSection("🔥 Em alta"),
  );
  panel.scrollTop = 0;
}

function renderCountry(data, props) {
  const errors = data.errors || {};
  panel.replaceChildren(
    header(props, data.info),
    facts(data.info),
    newsSection(data.news, errors.news),
    trendsSection(data.trends, errors.trends),
    historySection(data.onThisDay, errors.onThisDay),
    papersSection(data.newspapers),
    el("p", { className: "note", style: "text-align:center;margin-top:18px" },
      "Atualizado " + timeAgo(data.generatedAt)),
  );
}

function header(props, info) {
  return el("div", { className: "country-head" },
    info?.flagUrl ? el("img", { src: info.flagUrl, alt: info.flagAlt || "Bandeira" }) : null,
    el("div", {},
      el("h1", {}, info?.name || props.name),
      el("div", { className: "sub" }, [info?.subregion || info?.region, props.code].filter(Boolean).join(" · "))));
}

function facts(info) {
  if (!info) return null;
  const items = [
    ["Capital", info.capital],
    ["População", info.population ? fmtNumber.format(info.population) : null],
    ["Moeda", info.currencies?.join(", ")],
    ["Línguas", info.languages?.slice(0, 3).join(", ")],
    ["Área", info.area ? fmtNumber.format(Math.round(info.area)) + " km²" : null],
    ["Fuso horário", info.timezones?.length > 2 ? `${info.timezones[0]} (+${info.timezones.length - 1})` : info.timezones?.join(", ")],
  ].filter(([, value]) => value);

  return el("div", { className: "facts" },
    ...items.map(([label, value]) =>
      el("div", { className: "fact" }, el("div", { className: "label" }, label), el("div", { className: "value" }, value))));
}

function newsSection(news, error) {
  const hint = news?.mode === "search" ? "pesquisa global em inglês" : "edição local · Google News";
  const body = error ? el("p", { className: "error" }, error)
    : !news?.items?.length ? el("p", { className: "note" }, "Sem notícias encontradas.")
    : el("ul", { className: "list" },
        ...news.items.map((n) =>
          el("li", {},
            link(n.url, n.title),
            el("div", { className: "meta" }, [n.source, timeAgo(n.publishedAt)].filter(Boolean).join(" · ")))));

  return el("section", { className: "section" }, el("h2", {}, "📰 Notícias", el("span", { className: "hint" }, hint)), body);
}

function trendsSection(trends, error) {
  const body = error ? el("p", { className: "error" }, error)
    : !trends?.length ? el("p", { className: "note" }, "O Google Trends não tem dados para este país.")
    : el("div", { className: "chips" },
        ...trends.map((t) =>
          el("a", { className: "chip", href: t.url, target: "_blank", rel: "noopener noreferrer", title: t.newsTitle || "" },
            t.title,
            t.traffic ? el("span", { className: "traffic" }, t.traffic) : null)));

  return el("section", { className: "section" },
    el("h2", {}, "🔥 Em alta", el("span", { className: "hint" }, "pesquisas no Google hoje")), body);
}

function historySection(otd, error) {
  if (error) return el("section", { className: "section" }, el("h2", {}, "📅 Neste dia"), el("p", { className: "error" }, error));
  if (!otd) return null;

  const date = new Date(otd.date + "T12:00:00").toLocaleDateString("pt-PT", { day: "numeric", month: "long" });
  const events = otd.countryEvents?.length ? otd.countryEvents : otd.worldEvents || [];
  const note = otd.countryEvents?.length ? null
    : el("p", { className: "note", style: "margin-bottom:8px" }, "Nada ligado a este país hoje. No mundo:");

  return el("section", { className: "section" },
    el("h2", {}, "📅 Neste dia", el("span", { className: "hint" }, date + " · Wikipedia")),
    note,
    el("ul", { className: "list" },
      ...events.map((e) =>
        el("li", {}, el("span", { className: "year" }, String(e.year)), " ", e.url ? link(e.url, e.text) : e.text))));
}

function papersSection(papers) {
  const body = !papers?.length
    ? el("p", { className: "note" }, "Ainda não há jornais para este país. Acrescenta-os em data/newspapers.json.")
    : el("div", { className: "papers" },
        ...papers.map((p) =>
          el("a", { className: "paper", href: p.url, target: "_blank", rel: "noopener noreferrer" },
            el("div", {}, p.name),
            p.note ? el("div", { className: "meta" }, p.note) : null)));

  return el("section", { className: "section" }, el("h2", {}, "🗞️ Principais jornais"), body);
}

// ---------------------------------------------------------------------------
// Utilitários
// ---------------------------------------------------------------------------

/**
 * Cria elementos HTML de forma segura.
 * Todo o texto entra como texto (nunca como HTML), por isso um título de
 * notícia com <script> não consegue fazer nada.
 */
function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs)) {
    if (value == null) continue;
    if (key === "className") node.className = value;
    else if (key === "style") node.style.cssText = value;
    else node.setAttribute(key, value);
  }
  for (const child of children.flat()) {
    if (child == null || child === false) continue;
    node.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
  return node;
}

function link(url, text) {
  // Só aceitamos http/https (bloqueia "javascript:" e afins)
  if (!url || !/^https?:\/\//i.test(url)) return el("span", {}, text);
  return el("a", { href: url, target: "_blank", rel: "noopener noreferrer" }, text);
}

function timeAgo(iso) {
  if (!iso) return null;
  const seconds = (new Date(iso).getTime() - Date.now()) / 1000;
  const units = [["day", 86400], ["hour", 3600], ["minute", 60]];
  for (const [unit, size] of units) {
    if (Math.abs(seconds) >= size) return fmtRelative.format(Math.round(seconds / size), unit);
  }
  return "agora mesmo";
}

initMap();
