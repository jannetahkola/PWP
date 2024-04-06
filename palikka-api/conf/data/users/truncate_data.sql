-- https://www.postgresqltutorial.com/postgresql-tutorial/postgresql-truncate-table/
truncate table
    palikka_user,
    palikka_role,
    palikka_privilege
    restart identity
    cascade;