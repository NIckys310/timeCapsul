-- =========================================================
-- TIME CAPSULE · Despertador
-- Supabase pausa los proyectos gratis tras 7 días sin consultas
-- a la base de datos. La tarea automática de GitHub y la página
-- de estado llaman a ping(): es una consulta real (cuenta como uso)
-- pero no lee ni cambia ningún dato.
-- =========================================================
create or replace function public.ping()
returns timestamptz
language sql
stable
set search_path = public
as $$ select now() $$;

revoke all on function public.ping() from public;
grant execute on function public.ping() to anon, authenticated;
