begin;

-- Supabase Auth owns credentials and sessions in auth.users.
-- This public table stores only app-facing, non-sensitive profile settings.
create table if not exists public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    display_name text not null default '',
    daily_step_target integer not null default 10000,
    unit_system text not null default 'metric',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint profiles_display_name_length
        check (char_length(display_name) <= 80),
    constraint profiles_daily_step_target_range
        check (daily_step_target between 0 and 100000),
    constraint profiles_unit_system_values
        check (unit_system in ('metric', 'imperial'))
);

comment on table public.profiles is
    'Non-sensitive app profile settings for authenticated users.';
comment on column public.profiles.id is
    'Matches auth.users.id. The mobile app must never choose another user id.';

-- Keep the public Data API closed by default. RLS policies below grant each
-- signed-in user access only to their own row.
alter table public.profiles enable row level security;
revoke all on table public.profiles from anon, authenticated;
grant select, insert, update on table public.profiles to authenticated;

drop policy if exists profiles_select_own on public.profiles;
create policy profiles_select_own
on public.profiles
for select
to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = id);

drop policy if exists profiles_insert_own on public.profiles;
create policy profiles_insert_own
on public.profiles
for insert
to authenticated
with check ((select auth.uid()) is not null and (select auth.uid()) = id);

drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own
on public.profiles
for update
to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = id)
with check ((select auth.uid()) is not null and (select auth.uid()) = id);

create or replace function public.set_profile_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists profiles_set_updated_at on public.profiles;
create trigger profiles_set_updated_at
before update on public.profiles
for each row execute procedure public.set_profile_updated_at();

-- Create the matching public profile after a Supabase Auth signup. A failure
-- in this trigger can block signup, so keep the operation small and idempotent.
create or replace function public.handle_new_user_profile()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
    requested_name text;
begin
    requested_name := trim(coalesce(new.raw_user_meta_data ->> 'display_name', ''));

    insert into public.profiles (id, display_name)
    values (new.id, left(requested_name, 80))
    on conflict (id) do nothing;

    return new;
end;
$$;

drop trigger if exists on_auth_user_created_profile on auth.users;
create trigger on_auth_user_created_profile
after insert on auth.users
for each row execute procedure public.handle_new_user_profile();

commit;
