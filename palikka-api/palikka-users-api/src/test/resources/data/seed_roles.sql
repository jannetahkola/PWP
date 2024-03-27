-- ROLES
-- INSERT statements should not contain IDs for auto-generation to work as expected

insert into palikka_role (name, description)
values ('ROLE_ADMIN', 'Access to view & modify all data'),
       ('ROLE_USER', 'Access to view & modify limited data'),
       ('ROLE_VIEWER', 'Access to view limited data');

insert into palikka_role_privilege (role_id, privilege_id)
select 1, pp.id from palikka_privilege pp;

insert into palikka_role_privilege (role_id, privilege_id)
select 2, pp.id from palikka_privilege pp where pp.name in (
    'help',
    'msg',
    'time',
    'weather'
);
