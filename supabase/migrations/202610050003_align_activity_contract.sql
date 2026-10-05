begin;

-- The activity implementation records explicit start/finish times. Rename the
-- original generic timestamp before any client code depends on that name.
do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
            and table_name = 'activity_sessions'
            and column_name = 'recorded_at'
    ) and not exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
            and table_name = 'activity_sessions'
            and column_name = 'started_at'
    ) then
        alter table public.activity_sessions
            rename column recorded_at to started_at;
    end if;
end;
$$;

alter table public.activity_sessions
    add column if not exists finished_at timestamptz;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'activity_sessions_finish_after_start'
            and conrelid = 'public.activity_sessions'::regclass
    ) then
        alter table public.activity_sessions
            add constraint activity_sessions_finish_after_start
            check (finished_at is null or finished_at >= started_at);
    end if;
end;
$$;

drop index if exists public.activity_sessions_owner_recorded_at_idx;
create index if not exists activity_sessions_owner_started_at_idx
    on public.activity_sessions (owner_id, started_at desc);

-- Pause/resume creates multiple route segments. sequence_number is ordered
-- inside a segment so the original route can be reconstructed exactly.
alter table public.route_points
    add column if not exists segment_number integer not null default 0;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'route_points_segment_nonnegative'
            and conrelid = 'public.route_points'::regclass
    ) then
        alter table public.route_points
            add constraint route_points_segment_nonnegative
            check (segment_number >= 0);
    end if;
end;
$$;

alter table public.route_points
    drop constraint if exists route_points_unique_sequence;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'route_points_unique_segment_sequence'
            and conrelid = 'public.route_points'::regclass
    ) then
        alter table public.route_points
            add constraint route_points_unique_segment_sequence
            unique (session_id, segment_number, sequence_number);
    end if;
end;
$$;

drop index if exists public.route_points_session_sequence_idx;
create index if not exists route_points_session_segment_sequence_idx
    on public.route_points (session_id, segment_number, sequence_number);

comment on column public.activity_sessions.started_at is
    'UTC timestamp at which the activity session started.';
comment on column public.activity_sessions.finished_at is
    'UTC timestamp at which the session completed; null while in progress.';
comment on column public.route_points.segment_number is
    'Zero-based route segment created by start/resume boundaries.';

commit;
