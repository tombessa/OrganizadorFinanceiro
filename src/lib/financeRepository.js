import { requireSupabase } from "./supabase.js";

async function sha256(value) {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

export async function saveImport({ session, accountName, accountType, source, filename, transactions }) {
  const client = requireSupabase();
  const userId = session.user.id;
  const normalizedName = accountName.trim();
  if (!normalizedName) throw new Error("Informe a conta ou cartão de origem.");

  let { data: account, error: accountError } = await client
    .from("accounts").select("id").eq("name", normalizedName).eq("type", accountType).maybeSingle();
  if (accountError) throw accountError;
  if (!account) {
    const created = await client.from("accounts").insert({ user_id: userId, name: normalizedName, type: accountType, institution: source }).select("id").single();
    if (created.error) throw created.error;
    account = created.data;
  }

  const imported = await client.from("imports").insert({
    user_id: userId, account_id: account.id, source, original_filename: filename
  }).select("id").single();
  if (imported.error) throw imported.error;

  const categoryNames = [...new Set(transactions.map((item) => item.category).filter((name) => name !== "A revisar"))];
  const categoryIds = new Map();
  for (const name of categoryNames) {
    const kind = transactions.find((item) => item.category === name)?.amount >= 0 ? "income" : "expense";
    const { data, error } = await client.from("categories").upsert({ user_id: userId, name, kind }, { onConflict: "user_id,name" }).select("id,name").single();
    if (error) throw error;
    categoryIds.set(data.name, data.id);
  }

  const rows = await Promise.all(transactions.map(async (item) => ({
    user_id: userId,
    account_id: account.id,
    import_id: imported.data.id,
    category_id: categoryIds.get(item.category) ?? null,
    transaction_date: item.isoDate,
    description: item.description || "Sem descrição",
    amount: item.amount,
    transaction_hash: await sha256([account.id, item.isoDate, item.description, item.amount].join("|")),
    status: item.category === "A revisar" ? "review" : "confirmed"
  })));

  const { error } = await client.from("transactions").upsert(rows, {
    onConflict: "user_id,account_id,transaction_hash",
    ignoreDuplicates: true
  });
  if (error) throw error;
  return { count: rows.length };
}
