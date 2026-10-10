-- =====================================================================
--  TIME CAPSULE · Actualización 3
--  - Contenido: notas de voz, canción del momento, plantillas y preguntas
--  - Compartir: reacciones, comentarios, cápsulas colaborativas,
--    invitaciones por enlace/QR y "abrir juntos"
--  - Lugares: rutas de cápsulas que se abren en orden
--  - Seguridad: bloqueo tras 5 claves incorrectas
--  Pega TODO en Supabase → SQL Editor → Run. Se puede ejecutar varias veces.
-- =====================================================================

-- ---------- 1. Columnas nuevas ----------
alter table public.capsules add column if not exists song_url      text;
alter table public.capsules add column if not exists song_title    text;
alter table public.capsules add column if not exists template      text;
alter table public.capsules add column if not exists questions     text[] not null default '{}';
alter table public.capsules add column if not exists open_together boolean not null default false;
alter table public.capsules add column if not exists route_id      uuid;
alter table public.capsules add column if not exists route_order   int;

alter table public.capsule_media drop constraint if exists capsule_media_kind_check;
alter table public.capsule_media add constraint capsule_media_kind_check check (kind in ('photo', 'video', 'audio'));
alter table public.capsule_media add column if not exists added_by      uuid default auth.uid();
alter table public.capsule_media add column if not exists added_by_name text;

alter table public.capsule_shares add column if not exists can_contribute boolean not null default false;

-- ---------- 2. Límite por cápsula: 20 fotos, 1 video y 5 notas de voz ----------
create or replace function public.check_media_limit() returns trigger
language plpgsql security definer set search_path = public as $$
declare n int;
begin
  select count(*) into n from public.capsule_media where capsule_id = new.capsule_id and kind = new.kind;
  if new.kind = 'photo' and n >= 20 then raise exception 'Máximo 20 fotos por cápsula'; end if;
  if new.kind = 'video' and n >= 1  then raise exception 'Máximo 1 video por cápsula'; end if;
  if new.kind = 'audio' and n >= 5  then raise exception 'Máximo 5 notas de voz por cápsula'; end if;
  return new;
end $$;
drop trigger if exists media_limit on public.capsule_media;
create trigger media_limit before insert on public.capsule_media for each row execute function public.check_media_limit();

-- ---------- 3. Tablas nuevas ----------
create table if not exists public.capsule_answers (     -- respuestas a las preguntas para el futuro
  capsule_id uuid not null references public.capsules on delete cascade,
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  idx        int  not null,
  answer     text not null,
  created_at timestamptz not null default now(),
  primary key (capsule_id, user_id, idx)
);
create table if not exists public.capsule_reactions (
  capsule_id uuid not null references public.capsules on delete cascade,
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  emoji      text not null check (char_length(emoji) <= 8),
  created_at timestamptz not null default now(),
  primary key (capsule_id, user_id)
);
create table if not exists public.capsule_comments (
  id         uuid primary key default gen_random_uuid(),
  capsule_id uuid not null references public.capsules on delete cascade,
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  user_name  text,
  body       text not null check (char_length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);
create table if not exists public.capsule_letters (     -- cartas de los colaboradores
  id         uuid primary key default gen_random_uuid(),
  capsule_id uuid not null references public.capsules on delete cascade,
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  user_name  text,
  body       text not null check (char_length(body) between 1 and 8000),
  created_at timestamptz not null default now()
);
create table if not exists public.capsule_invites (     -- invitación por enlace o QR
  code           text primary key,
  capsule_id     uuid not null references public.capsules on delete cascade,
  created_by     uuid not null default auth.uid() references auth.users on delete cascade,
  can_contribute boolean not null default false,
  expires_at     timestamptz not null default now() + interval '7 days',
  uses           int not null default 0
);
create table if not exists public.capsule_open_votes (  -- "abrir juntos"
  capsule_id uuid not null references public.capsules on delete cascade,
  user_id    uuid not null default auth.uid() references auth.users on delete cascade,
  created_at timestamptz not null default now(),
  primary key (capsule_id, user_id)
);
create table if not exists public.capsule_routes (      -- rutas de cápsulas en orden
  id         uuid primary key default gen_random_uuid(),
  owner      uuid not null default auth.uid() references auth.users on delete cascade,
  title      text not null,
  created_at timestamptz not null default now()
);
create table if not exists public.key_attempts (        -- intentos fallidos de clave
  id         bigint generated always as identity primary key,
  capsule_id uuid not null,
  user_id    uuid not null,
  created_at timestamptz not null default now()
);
create index if not exists key_attempts_idx on public.key_attempts (capsule_id, user_id, created_at);

alter table public.capsule_answers    enable row level security;
alter table public.capsule_reactions  enable row level security;
alter table public.capsule_comments   enable row level security;
alter table public.capsule_letters    enable row level security;
alter table public.capsule_invites    enable row level security;
alter table public.capsule_open_votes enable row level security;
alter table public.capsule_routes     enable row level security;
alter table public.key_attempts       enable row level security;   -- sin políticas: solo funciones

-- ---------- 4. Reglas de apertura (fecha, lugar, clave, abrir juntos y ruta) ----------
create or replace function public.is_contributor(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.capsule_shares where capsule_id = cid and user_id = auth.uid() and can_contribute);
$$;

-- Todos los miembros (dueño + invitados) votaron "abrir"
create or replace function public.all_voted(cid uuid) returns boolean
language sql stable security definer set search_path = public as $$
  select (select count(*) from public.capsule_open_votes v where v.capsule_id = cid)
      >= 1 + (select count(*) from public.capsule_shares s where s.capsule_id = cid);
$$;

-- En una ruta, la cápsula anterior debe estar abierta
create or replace function public.route_prev_open(c public.capsules) returns boolean
language sql stable security definer set search_path = public as $$
  select c.route_id is null or c.route_order is null or c.route_order <= 1 or exists (
    select 1 from public.capsules p
     where p.route_id = c.route_id and p.route_order = c.route_order - 1
       and (p.early_open or ((p.unlock_at is null or p.unlock_at <= now()) and (not p.require_place or p.opened))));
$$;

create or replace function public.capsule_is_open(c public.capsules) returns boolean
language sql stable security definer set search_path = public as $$
  select c.early_open
      or ((c.unlock_at is null or c.unlock_at <= now())
          and (not c.require_place or c.opened)
          and (not c.open_together or public.all_voted(c.id))
          and public.route_prev_open(c));
$$;

-- ---------- 5. Políticas ----------
drop policy if exists "agregar recuerdos" on public.capsule_media;
create policy "agregar recuerdos" on public.capsule_media for insert to authenticated
  with check (public.is_owner(capsule_id) or (public.is_contributor(capsule_id) and not public.is_open_by_id(capsule_id)));

drop policy if exists "respuestas: ver" on public.capsule_answers;
drop policy if exists "respuestas: escribir" on public.capsule_answers;
create policy "respuestas: ver" on public.capsule_answers for select to authenticated using (public.can_view(capsule_id) and public.is_open_by_id(capsule_id));
create policy "respuestas: escribir" on public.capsule_answers for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid() and public.can_view(capsule_id) and public.is_open_by_id(capsule_id));

drop policy if exists "reacciones: ver" on public.capsule_reactions;
drop policy if exists "reacciones: mías" on public.capsule_reactions;
create policy "reacciones: ver" on public.capsule_reactions for select to authenticated using (public.can_view(capsule_id));
create policy "reacciones: mías" on public.capsule_reactions for all to authenticated
  using (user_id = auth.uid()) with check (user_id = auth.uid() and public.can_view(capsule_id) and public.is_open_by_id(capsule_id));

drop policy if exists "comentarios: ver" on public.capsule_comments;
drop policy if exists "comentarios: escribir" on public.capsule_comments;
drop policy if exists "comentarios: borrar" on public.capsule_comments;
create policy "comentarios: ver" on public.capsule_comments for select to authenticated using (public.can_view(capsule_id));
create policy "comentarios: escribir" on public.capsule_comments for insert to authenticated
  with check (user_id = auth.uid() and public.can_view(capsule_id) and public.is_open_by_id(capsule_id));
create policy "comentarios: borrar" on public.capsule_comments for delete to authenticated using (user_id = auth.uid() or public.is_owner(capsule_id));

drop policy if exists "cartas: ver" on public.capsule_letters;
drop policy if exists "cartas: escribir" on public.capsule_letters;
create policy "cartas: ver" on public.capsule_letters for select to authenticated
  using (public.can_view(capsule_id) and (public.is_open_by_id(capsule_id) or user_id = auth.uid()));
create policy "cartas: escribir" on public.capsule_letters for insert to authenticated
  with check (user_id = auth.uid() and (public.is_owner(capsule_id) or public.is_contributor(capsule_id)) and not public.is_open_by_id(capsule_id));

drop policy if exists "invitaciones: dueño" on public.capsule_invites;
create policy "invitaciones: dueño" on public.capsule_invites for all to authenticated
  using (public.is_owner(capsule_id)) with check (public.is_owner(capsule_id));

drop policy if exists "votos: ver" on public.capsule_open_votes;
create policy "votos: ver" on public.capsule_open_votes for select to authenticated using (public.can_view(capsule_id));

drop policy if exists "rutas: dueño" on public.capsule_routes;
drop policy if exists "rutas: ver" on public.capsule_routes;
create policy "rutas: dueño" on public.capsule_routes for all to authenticated using (owner = auth.uid()) with check (owner = auth.uid());
create policy "rutas: ver" on public.capsule_routes for select to authenticated
  using (exists (select 1 from public.capsules c where c.route_id = capsule_routes.id and public.can_view(c.id)));

-- ---------- 6. Vista segura (con todo lo nuevo) ----------
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
       c.early_open,
       case when public.capsule_is_open(c) then c.song_url end   as song_url,
       case when public.capsule_is_open(c) then c.song_title end as song_title,
       c.template,
       case when public.capsule_is_open(c) then c.questions else '{}'::text[] end as questions,
       cardinality(c.questions) as question_count,
       c.open_together,
       (select count(*) from public.capsule_open_votes v where v.capsule_id = c.id)::int as votes,
       1 + (select count(*) from public.capsule_shares s where s.capsule_id = c.id)::int as members,
       exists (select 1 from public.capsule_open_votes v where v.capsule_id = c.id and v.user_id = auth.uid()) as my_vote,
       c.route_id, c.route_order,
       (select r.title from public.capsule_routes r where r.id = c.route_id) as route_title,
       (c.owner = auth.uid() or public.is_contributor(c.id)) as can_add,
       (select count(*) from public.capsule_reactions r where r.capsule_id = c.id)::int as reaction_count,
       (select count(*) from public.capsule_comments k where k.capsule_id = c.id)::int as comment_count
from public.capsules c
where public.can_view(c.id);
revoke all on public.capsules_feed from anon;
grant select on public.capsules_feed to authenticated;

-- ---------- 7. Acciones ----------
-- Abrir juntos: cada miembro vota cuando llega la fecha (y el lugar si aplica)
create or replace function public.vote_open(cid uuid) returns boolean
language plpgsql security definer set search_path = public as $$
begin
  if not public.can_view(cid) then return false; end if;
  insert into public.capsule_open_votes (capsule_id, user_id) values (cid, auth.uid()) on conflict do nothing;
  return public.is_open_by_id(cid);
end $$;

-- Invitar por enlace o QR (solo el dueño). Devuelve el código.
create or replace function public.create_invite(cid uuid, contribute boolean default false) returns text
language plpgsql security definer set search_path = public as $$
declare code text;
begin
  if not public.is_owner(cid) then raise exception 'Solo el dueño puede invitar'; end if;
  code := upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 8));
  insert into public.capsule_invites (code, capsule_id, can_contribute) values (code, cid, contribute);
  return code;
end $$;

-- Aceptar una invitación. Devuelve el id de la cápsula.
create or replace function public.accept_invite(p_code text) returns uuid
language plpgsql security definer set search_path = public as $$
declare inv public.capsule_invites%rowtype;
begin
  select * into inv from public.capsule_invites where code = upper(trim(p_code)) and expires_at > now();
  if not found then raise exception 'La invitación no existe o ya venció'; end if;
  if not public.is_owner(inv.capsule_id) then
    insert into public.capsule_shares (capsule_id, user_id, can_contribute) values (inv.capsule_id, auth.uid(), inv.can_contribute)
    on conflict (capsule_id, user_id) do update set can_contribute = public.capsule_shares.can_contribute or excluded.can_contribute;
  end if;
  update public.capsule_invites set uses = uses + 1 where code = inv.code;
  return inv.capsule_id;
end $$;

-- Compartir con correo indicando si puede aportar
create or replace function public.share_with(cid uuid, e text, contribute boolean) returns text
language plpgsql security definer set search_path = public as $$
declare u record;
begin
  if not public.is_owner(cid) then raise exception 'Solo el dueño puede compartir'; end if;
  select id, name into u from public.profiles where lower(email) = lower(trim(e)) and id <> auth.uid();
  if not found then raise exception 'No hay ninguna cuenta con %. Pídele que se registre en Time Capsule.', e; end if;
  insert into public.capsule_shares (capsule_id, user_id, can_contribute) values (cid, u.id, contribute)
  on conflict (capsule_id, user_id) do update set can_contribute = excluded.can_contribute;
  return coalesce(u.name, e);
end $$;

-- Abrir con clave: ahora bloquea 15 minutos tras 5 intentos fallidos
create or replace function public.open_with_key(cid uuid, k text) returns boolean
language plpgsql security definer set search_path = public, extensions as $$
declare h text; fails int;
begin
  if not public.can_view(cid) then return false; end if;
  select count(*) into fails from public.key_attempts
   where capsule_id = cid and user_id = auth.uid() and created_at > now() - interval '15 minutes';
  if fails >= 5 then raise exception 'Demasiados intentos. Espera 15 minutos para volver a probar.'; end if;
  select key_hash into h from public.capsules where id = cid;
  if h is null or k is null or extensions.crypt(k, h) <> h then
    insert into public.key_attempts (capsule_id, user_id) values (cid, auth.uid());
    perform pg_sleep(1);
    return false;
  end if;
  delete from public.key_attempts where capsule_id = cid and user_id = auth.uid();
  update public.capsules set early_open = true where id = cid;
  return true;
end $$;

-- Actividad reciente en mis cápsulas (para los avisos de amigos)
create or replace function public.recent_activity(since timestamptz) returns table (kind text, capsule_id uuid, title text, who text, body text, at timestamptz)
language sql stable security definer set search_path = public as $$
  select 'comment', k.capsule_id, c.title, k.user_name, k.body, k.created_at
    from public.capsule_comments k join public.capsules c on c.id = k.capsule_id
   where k.created_at > since and k.user_id <> auth.uid() and public.can_view(c.id)
  union all
  select 'reaction', r.capsule_id, c.title, p.name, r.emoji, r.created_at
    from public.capsule_reactions r join public.capsules c on c.id = r.capsule_id left join public.profiles p on p.id = r.user_id
   where r.created_at > since and r.user_id <> auth.uid() and public.can_view(c.id)
  order by 6 desc limit 30;
$$;

revoke execute on function public.vote_open(uuid), public.create_invite(uuid, boolean), public.accept_invite(text),
  public.share_with(uuid, text, boolean), public.open_with_key(uuid, text), public.recent_activity(timestamptz) from anon;

-- Los colaboradores también pueden ver el video/nota que ellos subieron antes de abrirse
drop policy if exists "ver mis aportes" on public.capsule_media;
create policy "ver mis aportes" on public.capsule_media for select to authenticated using (added_by = auth.uid());
