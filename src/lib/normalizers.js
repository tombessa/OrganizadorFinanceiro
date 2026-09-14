export function parseBrazilianAmount(value) {
  const clean = String(value ?? "").replace(/R\$\s?/g, "").replace(/\./g, "").replace(",", ".").trim();
  const number = Number(clean);
  return Number.isFinite(number) ? number : 0;
}

function toIsoDate(value) {
  const date = String(value ?? "").trim();
  const brazilian = date.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
  if (brazilian) return `${brazilian[3]}-${brazilian[2]}-${brazilian[1]}`;
  return /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : null;
}

export function inferCategory(description) {
  const normalized = description.toUpperCase();
  if (/ALUGUEL|CONDOMINIO/.test(normalized)) return "Moradia";
  if (/MERCADO|SUPERMERCADO|IFOOD|RESTAURANTE/.test(normalized)) return "Alimentação";
  if (/VIVO|CLARO|INTERNET|TELEFONE/.test(normalized)) return "Comunicação";
  if (/UBER|99|METRO|TRANSPORTE/.test(normalized)) return "Transporte";
  if (/UNIMED|FARMACIA|DROGARIA|MEDIC/.test(normalized)) return "Saúde";
  return "A revisar";
}

export function normalizeRows(rows) {
  return rows.map((row, index) => {
    const description = row["Descrição"] || row["Descricao"] || row["description"] || row["Estabelecimento"] || row["Lançamento"] || "";
    const rawAmount = row["Valor"] || row["value"] || row["amount"] || row["Valor (R$)"] || "0";
    const amount = parseBrazilianAmount(rawAmount);
    return {
      id: `${index}-${description}-${amount}`,
      date: row["Data"] || row["date"] || row["Data da compra"] || "",
      isoDate: toIsoDate(row["Data"] || row["date"] || row["Data da compra"]),
      description, amount,
      direction: amount >= 0 ? "entrada" : "saída",
      category: inferCategory(description)
    };
  }).filter((item) => item.amount !== 0);
}
