export function parseBrazilianAmount(value) {
  const clean = String(value ?? "").replace(/R\$\s?/g, "").replace(/\./g, "").replace(",", ".").trim();
  const number = Number(clean);
  return Number.isFinite(number) ? number : 0;
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
    const description = row["Descrição"] || row["Descricao"] || row["description"] || row["Estabelecimento"] || "";
    const rawAmount = row["Valor"] || row["value"] || row["amount"] || "0";
    const amount = parseBrazilianAmount(rawAmount);
    return {
      id: `${index}-${description}-${amount}`,
      date: row["Data"] || row["date"] || "",
      description,
      amount,
      direction: amount >= 0 ? "entrada" : "saída",
      category: inferCategory(description)
    };
  });
}
