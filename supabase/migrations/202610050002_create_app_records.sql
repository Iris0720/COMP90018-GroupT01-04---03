begin;

-- Remote records use client-generated UUIDs so Room and Supabase can upsert
-- the same logical record after offline work. Timestamps are stored in UTC by
-- PostgreSQL's timestamptz type.

create table if not exists public.activity_sessions (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    recorded_at timestamptz not null,
    activity_type text not null,
    session_state text not null default 'complete',
    goal_type text,
    goal_value bigint,
    duration_seconds bigint not null default 0,
    distance_metres bigint not null default 0,
    steps bigint,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint activity_sessions_owner_identity unique (id, owner_id),
    constraint activity_sessions_type_values
        check (activity_type in ('walking', 'running', 'hiking')),
    constraint activity_sessions_state_values
        check (session_state in ('setup', 'live', 'paused', 'complete')),
    constraint activity_sessions_goal_type_values
        check (goal_type is null or goal_type in ('time', 'distance', 'open')),
    constraint activity_sessions_goal_value_nonnegative
        check (goal_value is null or goal_value >= 0),
    constraint activity_sessions_duration_nonnegative
        check (duration_seconds >= 0),
    constraint activity_sessions_distance_nonnegative
        check (distance_metres >= 0),
    constraint activity_sessions_steps_nonnegative
        check (steps is null or steps >= 0)
);

create index if not exists activity_sessions_owner_recorded_at_idx
    on public.activity_sessions (owner_id, recorded_at desc);

create table if not exists public.route_points (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    session_id uuid not null,
    sequence_number integer not null,
    recorded_at timestamptz not null,
    latitude double precision not null,
    longitude double precision not null,
    accuracy_metres real,
    altitude_metres real,
    speed_metres_per_second real,
    created_at timestamptz not null default now(),
    constraint route_points_session_owner_fk
        foreign key (session_id, owner_id)
        references public.activity_sessions (id, owner_id)
        on delete cascade,
    constraint route_points_sequence_nonnegative
        check (sequence_number >= 0),
    constraint route_points_unique_sequence
        unique (session_id, sequence_number),
    constraint route_points_latitude_range
        check (latitude between -90 and 90),
    constraint route_points_longitude_range
        check (longitude between -180 and 180),
    constraint route_points_accuracy_nonnegative
        check (accuracy_metres is null or accuracy_metres >= 0),
    constraint route_points_speed_nonnegative
        check (speed_metres_per_second is null or speed_metres_per_second >= 0)
);

create index if not exists route_points_session_sequence_idx
    on public.route_points (session_id, sequence_number);

create table if not exists public.health_check_ins (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    recorded_at timestamptz not null,
    feeling text not null,
    breathing_difficulty text not null,
    fatigue text not null,
    condition text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint health_check_ins_feeling_values
        check (feeling in ('good', 'okay', 'unwell')),
    constraint health_check_ins_breathing_values
        check (breathing_difficulty in ('none', 'mild', 'severe')),
    constraint health_check_ins_fatigue_values
        check (fatigue in ('none', 'mild', 'severe')),
    constraint health_check_ins_condition_values
        check (condition in ('good', 'caution', 'unknown'))
);

create index if not exists health_check_ins_owner_recorded_at_idx
    on public.health_check_ins (owner_id, recorded_at desc);

create table if not exists public.meal_entries (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
    recorded_at timestamptz not null,
    name text not null,
    serving text not null,
    calories integer not null default 0,
    protein_grams integer not null default 0,
    carbohydrate_grams integer not null default 0,
    fat_grams integer not null default 0,
    source_label text not null default 'Manual entry',
    photo_storage_path text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint meal_entries_name_length
        check (char_length(name) between 1 and 160),
    constraint meal_entries_serving_length
        check (char_length(serving) between 1 and 160),
    constraint meal_entries_calories_nonnegative
        check (calories >= 0),
    constraint meal_entries_protein_nonnegative
        check (protein_grams >= 0),
    constraint meal_entries_carbohydrate_nonnegative
        check (carbohydrate_grams >= 0),
    constraint meal_entries_fat_nonnegative
        check (fat_grams >= 0)
);

create index if not exists meal_entries_owner_recorded_at_idx
    on public.meal_entries (owner_id, recorded_at desc);

-- Keep updated_at server-controlled for rows that can change.
create or replace function public.set_app_record_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists activity_sessions_set_updated_at on public.activity_sessions;
create trigger activity_sessions_set_updated_at
before update on public.activity_sessions
for each row execute procedure public.set_app_record_updated_at();

drop trigger if exists health_check_ins_set_updated_at on public.health_check_ins;
create trigger health_check_ins_set_updated_at
before update on public.health_check_ins
for each row execute procedure public.set_app_record_updated_at();

drop trigger if exists meal_entries_set_updated_at on public.meal_entries;
create trigger meal_entries_set_updated_at
before update on public.meal_entries
for each row execute procedure public.set_app_record_updated_at();

-- Exposed Data API tables are closed to anonymous users and protected with
-- one policy per operation. Every policy binds owner_id to the Auth JWT.
alter table public.activity_sessions enable row level security;
alter table public.route_points enable row level security;
alter table public.health_check_ins enable row level security;
alter table public.meal_entries enable row level security;

revoke all on table public.activity_sessions from anon, authenticated;
revoke all on table public.route_points from anon, authenticated;
revoke all on table public.health_check_ins from anon, authenticated;
revoke all on table public.meal_entries from anon, authenticated;

grant select, insert, update, delete on table public.activity_sessions to authenticated;
grant select, insert, update, delete on table public.route_points to authenticated;
grant select, insert, update, delete on table public.health_check_ins to authenticated;
grant select, insert, update, delete on table public.meal_entries to authenticated;

drop policy if exists activity_sessions_select_own on public.activity_sessions;
create policy activity_sessions_select_own on public.activity_sessions
for select to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists activity_sessions_insert_own on public.activity_sessions;
create policy activity_sessions_insert_own on public.activity_sessions
for insert to authenticated
with check ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists activity_sessions_update_own on public.activity_sessions;
create policy activity_sessions_update_own on public.activity_sessions
for update to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id)
with check ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists activity_sessions_delete_own on public.activity_sessions;
create policy activity_sessions_delete_own on public.activity_sessions
for delete to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists route_points_select_own on public.route_points;
create policy route_points_select_own on public.route_points
for select to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists route_points_insert_own on public.route_points;
create policy route_points_insert_own on public.route_points
for insert to authenticated
with check ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists route_points_update_own on public.route_points;
create policy route_points_update_own on public.route_points
for update to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id)
with check ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists route_points_delete_own on public.route_points;
create policy route_points_delete_own on public.route_points
for delete to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists health_check_ins_select_own on public.health_check_ins;
create policy health_check_ins_select_own on public.health_check_ins
for select to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists health_check_ins_insert_own on public.health_check_ins;
create policy health_check_ins_insert_own on public.health_check_ins
for insert to authenticated
with check ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists health_check_ins_update_own on public.health_check_ins;
create policy health_check_ins_update_own on public.health_check_ins
for update to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id)
with check ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists health_check_ins_delete_own on public.health_check_ins;
create policy health_check_ins_delete_own on public.health_check_ins
for delete to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists meal_entries_select_own on public.meal_entries;
create policy meal_entries_select_own on public.meal_entries
for select to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists meal_entries_insert_own on public.meal_entries;
create policy meal_entries_insert_own on public.meal_entries
for insert to authenticated
with check ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists meal_entries_update_own on public.meal_entries;
create policy meal_entries_update_own on public.meal_entries
for update to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id)
with check ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

drop policy if exists meal_entries_delete_own on public.meal_entries;
create policy meal_entries_delete_own on public.meal_entries
for delete to authenticated
using ((select auth.uid()) is not null and (select auth.uid()) = owner_id);

commit;
