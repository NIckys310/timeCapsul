-- =========================================================
-- TIME CAPSULE · Claves de cápsula más fuertes
-- La clave de una cápsula nunca se guarda: se guarda su huella bcrypt.
-- Antes: bcrypt con costo 6 (el de fábrica). Ahora: costo 12, unas
-- 64 veces más lento de adivinar por fuerza bruta.
-- Las huellas viejas siguen sirviendo y se mejoran solas la próxima
-- vez que alguien abre la cápsula con la clave correcta.
-- =========================================================

-- Poner la clave (solo el dueño): se guarda solo su huella bcrypt de costo 12
create or replace function public.set_capsule_key(cid uuid, k text) returns void
language plpgsql security definer set search_path = public, extensions as $$
begin
  if not public.is_owner(cid) then raise exception 'Solo el dueño puede poner la clave'; end if;
  if k is null or length(trim(k)) < 4 then raise exception 'La clave debe tener al menos 4 caracteres'; end if;
  update public.capsules set key_hash = extensions.crypt(k, extensions.gen_salt('bf', 12)) where id = cid;
end $$;

-- Abrir con la clave: bloquea 15 minutos tras 5 intentos fallidos y mejora huellas viejas
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
  -- Huella con costo menor a 12 ($2a$06$…): se rehace con la clave correcta que acaba de llegar
  if substr(h, 5, 2)::int < 12 then
    update public.capsules set key_hash = extensions.crypt(k, extensions.gen_salt('bf', 12)) where id = cid;
  end if;
  update public.capsules set early_open = true where id = cid;
  return true;
end $$;

revoke execute on function public.set_capsule_key(uuid, text), public.open_with_key(uuid, text) from anon;
