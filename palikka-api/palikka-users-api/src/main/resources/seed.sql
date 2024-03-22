insert into palikka_user (id, username, password, salt, active, root)
values (1, 'root', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true, true);

insert into palikka_user (id, username, password, salt, active, root)
values (2, 'demouser', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true, false);

insert into palikka_user (id, username, password, salt, active, root)
values (3, 'demoviewer', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true, false);

insert into palikka_role (id, name, description)
values (1, 'ROLE_ADMIN', 'Access to view & modify all data');
insert into palikka_role (id, name, description)
values (2, 'ROLE_USER', 'Access to view & modify limited data');
insert into palikka_role (id, name, description)
values (3, 'ROLE_VIEWER', 'Access to view limited data');

insert into palikka_user_role (user_id, role_id) values (1, 1);
insert into palikka_user_role (user_id, role_id) values (2, 2);
insert into palikka_user_role (user_id, role_id) values (3, 3);

