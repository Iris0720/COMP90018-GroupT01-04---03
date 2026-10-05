begin;

drop index if exists public.route_points_session_segment_sequence_idx;
alter table public.route_points
    drop constraint if exists route_points_unique_segment_sequence;
alter table public.route_points
    drop constraint if exists route_points_segment_nonnegative;
alter table public.route_points
    drop column if exists segment_number;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'route_points_unique_sequence'
            and conrelid = 'public.route_points'::regclass
    ) then
        alter table public.route_points
            add constraint route_points_unique_sequence
            unique (session_id, sequence_number);
    end if;
end;
$$;

create index if not exists route_points_session_sequence_idx
    on public.route_points (session_id, sequence_number);

drop index if exists public.activity_sessions_owner_started_at_idx;
alter table public.activity_sessions
    drop constraint if exists activity_sessions_finish_after_start;
alter table public.activity_sessions
    drop column if exists finished_at;

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
            and table_name = 'activity_sessions'
            and column_name = 'started_at'
    ) and not exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
            and table_name = 'activity_sessions'
            and column_name = 'recorded_at'
    ) then
        alter table public.activity_sessions
            rename column started_at to recorded_at;
    end if;
end;
$$;

create index if not exists activity_sessions_owner_recorded_at_idx
    on public.activity_sessions (owner_id, recorded_at desc);

commit;
