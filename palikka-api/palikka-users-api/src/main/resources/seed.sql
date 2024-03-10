insert into palikka_user (id, username, password, salt, active)
values (1, 'admin', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true);

insert into palikka_role (id, name) values (1, 'ROLE_ADMIN');

insert into palikka_user_role (user_id, role_id) values (1, 1);

