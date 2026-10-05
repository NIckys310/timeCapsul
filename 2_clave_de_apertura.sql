-- =====================================================================
--  TIME CAPSULE · Actualización 2: abrir cápsulas con clave
--  Pega TODO en Supabase → SQL Editor → New query → Run
--  (Se puede ejecutar varias veces sin problema)
-- =====================================================================

create extension if not exists pgcrypto with schema extensions;

-- Nuevas columnas: la clave se guarda cifrada (bcrypt), nunca en texto
alter table public.capsules add column if not exists key_hash   text;
alter table public.capsules add column if not exists early_open boolean not null default false;

-- Una cápsula está abierta si: se abrió con clave, O (llegó la fecha Y, si exige lugar, se abrió allí)
create or replace function public.capsule_is_open(c public.capsules) returns boolean
language sql stable as $$
  select c.early_open
      or ((c.unlock_at is null or c.unlock_at <= now()) and (not c.require_place or c.opened));
$$;

-- La vista ahora informa si la cápsula tiene clave
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
       end as cover_path,
       (c.key_hash is not null) as has_key,
       c.early_open
from public.capsules c
where public.can_view(c.id);
revoke all on public.capsules_feed from anon;
grant select on public.capsules_feed to authenticated;

-- Poner la clave (solo el dueño)
create or replace function public.set_capsule_key(cid uuid, k text) returns void
language plpgsql security definer set search_path = public, extensions as $$
begin
  if not public.is_owner(cid) then raise exception 'Solo el dueño puede poner la clave'; end if;
  if k is null or length(trim(k)) < 4 then raise exception 'La clave debe tener al menos 4 caracteres'; end if;
  update public.capsules set key_hash = extensions.crypt(k, extensions.gen_salt('bf')) where id = cid;
end $$;

-- Abrir con la clave (el dueño o alguien con quien se compartió)
create or replace function public.open_with_key(cid uuid, k text) returns boolean
language plpgsql security definer set search_path = public, extensions as $$
declare h text;
begin
  if not public.can_view(cid) then return false; end if;
  select key_hash into h from public.capsules where id = cid;
  if h is null or k is null or extensions.crypt(k, h) <> h then
    perform pg_sleep(1);   -- frena a quien intente adivinar la clave
    return false;
  end if;
  update public.capsules set early_open = true where id = cid;
  return true;
end $$;

revoke execute on function public.set_capsule_key(uuid, text), public.open_with_key(uuid, text) from anon;
