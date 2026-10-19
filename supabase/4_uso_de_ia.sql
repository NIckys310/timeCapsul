-- =========================================================
-- TIME CAPSULE · 4 · Uso de la IA (límite diario por persona)
-- La función de IA (Netlify) cuenta aquí cuántas consultas lleva cada
-- persona en las últimas 24 h. Cada quien solo ve y agrega las suyas.
-- =========================================================
create table if not exists public.ai_uso (
  id      bigint generated always as identity primary key,
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  accion  text not null check (accion in ('summary', 'timeline', 'suggest', 'letter', 'year_ago', 'album')),
  creado  timestamptz not null default now()
);
create index if not exists ai_uso_user_idx on public.ai_uso (user_id, creado desc);

alter table public.ai_uso enable row level security;

drop policy if exists "ia: ver mi uso" on public.ai_uso;
create policy "ia: ver mi uso" on public.ai_uso for select using (user_id = auth.uid());

drop policy if exists "ia: registrar mi uso" on public.ai_uso;
create policy "ia: registrar mi uso" on public.ai_uso for insert with check (user_id = auth.uid());

-- Limpieza: se borran los registros de más de 30 días (no se necesitan)
create or replace function public.limpiar_uso_ia()
returns void language sql security definer set search_path = public as $$
  delete from public.ai_uso where creado < now() - interval '30 days';
$$;
revoke execute on function public.limpiar_uso_ia() from anon, authenticated;
