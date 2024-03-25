insert into palikka_user (id, username, password, salt, active, root)
values (1, 'admin', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true, true),
       (2, 'user', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true, false),
       (3, 'viewer', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true, false);

insert into palikka_user_role (user_id, role_id)
values (1, 1),
       (2, 2),
       (3, 3);
