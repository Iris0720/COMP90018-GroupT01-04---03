select
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
from information_schema.columns
where table_schema = 'public'
    and (
        (table_name = 'activity_sessions'
            and column_name in ('started_at', 'finished_at'))
        or
        (table_name = 'route_points'
            and column_name = 'segment_number')
    )
order by table_name, ordinal_position;

-- Expected columns:
-- activity_sessions | started_at     | timestamp with time zone | NO
-- activity_sessions | finished_at    | timestamp with time zone | YES
-- route_points      | segment_number | integer                  | NO
