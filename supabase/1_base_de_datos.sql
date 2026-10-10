-- =====================================================================
--  TIME CAPSULE · Base de datos
--  Pega TODO este archivo en Supabase → SQL Editor → New query → Run
--  Se puede ejecutar varias veces sin romper nada.
-- =====================================================================

-- ---------- 1. Perfiles (se crean solos al registrarse) ----------
create table if not exists public.profiles (
  id    uuid primary key references auth.users on delete cascade,
  email text,
  name  text
);
alter table public.profiles enable row level security;
drop policy if exists "ver mi perfil" on public.profiles;
create policy "ver mi perfil" on public.profiles for select to authenticated using (id = auth.uid());

create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, email, name)
  values (new.id, new.email, coalesce(new.raw_user_meta_data->>'name', split_part(new.email, '@', 1)))
  on conflict (id) do nothing;
  return new;
end $$;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------- 2. Tablas ----------
create table if not exists public.capsules (
  id            uuid primary key default gen_random_uuid(),
  owner         uuid not null default auth.uid() references auth.users on delete cascade,
  owner_name    text,
  title         text not null,
  letter        text default '',
  created_at    timestamptz not null default now(),
  unlock_at     timestamptz,                 -- null = sin condición de fecha
  place_name    text,
  lat           double precision,
  lng           double precision,
  radius_m      int not null default 150,
  require_place boolean not null default false,  -- solo se abre estando en el lugar
  opened        boolean not null default false,  -- alguien la abrió en el lugar
  ai_summary    text
);

create table if not exists public.capsule_media (
  id         uuid primary key default gen_random_uuid(),
  capsule_id uuid not null references public.capsules on delete cascade,
  path       text not null unique,
  kind       text not null check (kind in ('photo', 'video')),
  created_at timestamptz not null default now()
);

create table if not exists public.capsule_shares (
  capsule_id uuid not null references public.capsules on delete cascade,
  user_id    uuid not null references auth.users on delete cascade,
  primary key (capsule_id, user_id)
);

alter table public.capsules       enable row level security;
alter table public.capsule_media  enable row level security;
alter table public.capsule_shares enable row level security;

-- ---------- 3. Funciones de apoyo ----------
create or replace function public.is_owner(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.capsules where id = cid and owner = auth.uid());
$$;

create or replace function public.can_view(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.capsules where id = cid and owner = auth.uid())
      or exists (select 1 from public.capsule_shares where capsule_id = cid and user_id = auth.uid());
$$;

-- ¿Ya se puede abrir? Fecha cumplida y, si exige lugar, que alguien la haya abierto allí.
create or replace function public.capsule_is_open(c public.capsules) returns boolean
language sql stable as $$
  select (c.unlock_at is null or c.unlock_at <= now()) and (not c.require_place or c.opened);
$$;

create or replace function public.is_open_by_id(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select coalesce((select public.capsule_is_open(c) from public.capsules c where c.id = cid), false);
$$;

-- ---------- 4. Reglas de acceso ----------
drop policy if exists "ver cápsulas"      on public.capsules;
drop policy if exists "crear cápsulas"    on public.capsules;
drop policy if exists "editar cápsulas"   on public.capsules;
drop policy if exists "borrar cápsulas"   on public.capsules;
create policy "ver cápsulas" on public.capsules for select to authenticated
  using (owner = auth.uid() or exists (select 1 from public.capsule_shares s where s.capsule_id = capsules.id and s.user_id = auth.uid()));
create policy "crear cápsulas"  on public.capsules for insert to authenticated with check (owner = auth.uid());
create policy "editar cápsulas" on public.capsules for update to authenticated using (owner = auth.uid());
create policy "borrar cápsulas" on public.capsules for delete to authenticated using (owner = auth.uid());

-- Nadie lee la carta directamente de la tabla: solo por la vista segura (que la oculta si está cerrada)
revoke select on public.capsules from anon, authenticated;
grant  select (id, owner) on public.capsules to authenticated;

drop policy if exists "ver recuerdos abiertos" on public.capsule_media;
drop policy if exists "agregar recuerdos"      on public.capsule_media;
drop policy if exists "borrar recuerdos"       on public.capsule_media;
create policy "ver recuerdos abiertos" on public.capsule_media for select to authenticated
  using (public.can_view(capsule_id) and public.is_open_by_id(capsule_id));
create policy "agregar recuerdos" on public.capsule_media for insert to authenticated with check (public.is_owner(capsule_id));
create policy "borrar recuerdos"  on public.capsule_media for delete to authenticated using (public.is_owner(capsule_id));

drop policy if exists "ver compartidos"   on public.capsule_shares;
drop policy if exists "compartir"         on public.capsule_shares;
drop policy if exists "dejar de compartir" on public.capsule_shares;
create policy "ver compartidos" on public.capsule_shares for select to authenticated
  using (user_id = auth.uid() or public.is_owner(capsule_id));
create policy "compartir"          on public.capsule_shares for insert to authenticated with check (public.is_owner(capsule_id));
create policy "dejar de compartir" on public.capsule_shares for delete to authenticated using (public.is_owner(capsule_id));

-- ---------- 5. Vista segura que usa la app ----------
drop view if exists public.capsules_feed;
create view public.capsules_feed as
select c.id, c.owner, c.owner_name, c.title, c.created_at, c.unlock_at,
       c.place_name, c.lat, c.lng, c.radius_m, c.require_place, c.opened,
       public.capsule_is_open(c) as is_open,
       case when public.capsule_is_open(c) then c.letter end      as letter,
       case when public.capsule_is_open(c) then c.ai_summary end  as ai_summary,
       (select count(*) from public.capsule_media m where m.capsule_id = c.id)::int as media_count,
       case when public.capsule_is_open(c) then
         (select m.path from public.capsule_media m where m.capsule_id = c.id and m.kind = 'photo' order by m.created_at limit 1)
       end as cover_path
from public.capsules c
where public.can_view(c.id);
revoke all on public.capsules_feed from anon;
grant select on public.capsules_feed to authenticated;

-- ---------- 6. Acciones que llama la app ----------
-- Abrir una cápsula que exige lugar (el servidor revisa que ya llegó la fecha)
create or replace function public.open_capsule(cid uuid) returns boolean
language plpgsql security definer set search_path = public as $$
begin
  if not public.can_view(cid) then return false; end if;
  update public.capsules set opened = true
   where id = cid and (unlock_at is null or unlock_at <= now());
  return found;
end $$;

-- Buscar a alguien por correo para compartir (devuelve solo id y nombre)
create or replace function public.find_user(e text) returns table (id uuid, name text)
language sql stable security definer set search_path = public as $$
  select p.id, p.name from public.profiles p
   where lower(p.email) = lower(trim(e)) and p.id <> auth.uid() limit 1;
$$;

-- Con quién está compartida una cápsula (solo lo ve el dueño)
create or replace function public.capsule_members(cid uuid) returns table (name text, email text)
language sql stable security definer set search_path = public as $$
  select p.name, p.email from public.capsule_shares s join public.profiles p on p.id = s.user_id
   where s.capsule_id = cid and public.is_owner(cid);
$$;

revoke execute on function public.open_capsule(uuid), public.find_user(text), public.capsule_members(uuid) from anon;

-- ---------- 7. Archivos (fotos y videos) ----------
insert into storage.buckets (id, name, public, file_size_limit)
values ('media', 'media', false, 52428800)
on conflict (id) do nothing;

drop policy if exists "subir a mi carpeta"     on storage.objects;
drop policy if exists "ver recuerdos abiertos" on storage.objects;
drop policy if exists "borrar mis archivos"    on storage.objects;
create policy "subir a mi carpeta" on storage.objects for insert to authenticated
  with check (bucket_id = 'media' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "ver recuerdos abiertos" on storage.objects for select to authenticated
  using (bucket_id = 'media' and exists (select 1 from public.capsule_media m where m.path = objects.name));
create policy "borrar mis archivos" on storage.objects for delete to authenticated
  using (bucket_id = 'media' and (storage.foldername(name))[1] = auth.uid()::text);
