import { createClient } from "@supabase/supabase-js";

const url = import.meta.env.VITE_SUPABASE_URL;
const key = import.meta.env.VITE_SUPABASE_ANON_KEY;

// Publishable by design. It is forwarded only so Supabase's API gateway can
// evaluate the authenticated user's Storage RLS policy.
export const supabaseApiKey = key || "";

export const supabase = url && key
  ? createClient(url, key, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true,
      },
    })
  : null;

export function requireSupabase() {
  if (!supabase) {
    throw new Error("Configure VITE_SUPABASE_URL e VITE_SUPABASE_ANON_KEY.");
  }
  return supabase;
}
