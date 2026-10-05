select
    t.tablename,
    t.rowsecurity as rls_enabled,
    count(p.policyname) as policy_count
from pg_tables t
left join pg_policies p
    on p.schemaname = t.schemaname
    and p.tablename = t.tablename
where t.schemaname = 'public'
    and t.tablename in (
        'profiles',
        'activity_sessions',
        'route_points',
        'health_check_ins',
        'meal_entries'
    )
group by t.tablename, t.rowsecurity
order by t.tablename;

-- Expected result after applying both migrations:
--
-- activity_sessions | true | 4
-- health_check_ins   | true | 4
-- meal_entries      | true | 4
-- profiles          | true | 3
-- route_points      | true | 4
