-- USERS
-- INSERT statements should not contain IDs for auto-generation to work as expected

insert into palikka_user (username, password, salt, active, root)
values ('admin', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true, true),
       ('user', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true, false),
       ('viewer', 'AMpMjzUFd7nR6Pc4l3BTmeOMQmJuLDQEHr7qp82QLt0=', 'pa2agTlplJ9FsYEmElH4iA==', true, false)
;

insert into palikka_user_role (user_id, role_id)
values (1, 1),
       (2, 2),
       (3, 3)
;