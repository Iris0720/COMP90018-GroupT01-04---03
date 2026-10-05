begin;

drop trigger if exists meal_entries_set_updated_at on public.meal_entries;
drop trigger if exists health_check_ins_set_updated_at on public.health_check_ins;
drop trigger if exists activity_sessions_set_updated_at on public.activity_sessions;

drop table if exists public.route_points;
drop table if exists public.meal_entries;
drop table if exists public.health_check_ins;
drop table if exists public.activity_sessions;

drop function if exists public.set_app_record_updated_at();

commit;
