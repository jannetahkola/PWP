insert into palikka_role (id, name, description)
values (1, 'ROLE_ADMIN', 'Access to view & modify all data'),
       (2, 'ROLE_USER', 'Access to view & modify limited data'),
       (3, 'ROLE_VIEWER', 'Access to view limited data');

insert into palikka_role_privilege (role_id, privilege_id)
select 1, pp.id from palikka_privilege pp;

insert into palikka_role_privilege (role_id, privilege_id)
select 2, pp.id from palikka_privilege pp where pp.name in (
    'help',
    'msg',
    'time',
    'weather'
);
