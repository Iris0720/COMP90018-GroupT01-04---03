begin;

drop trigger if exists on_auth_user_created_profile on auth.users;
drop function if exists public.handle_new_user_profile();

drop trigger if exists profiles_set_updated_at on public.profiles;
drop function if exists public.set_profile_updated_at();

drop table if exists public.profiles;

commit;
