-- Truncate all tables and reset sequences

truncate table palikka_user restart identity cascade;
truncate table palikka_role restart identity cascade;
truncate table palikka_privilege restart identity cascade;
truncate table palikka_user_role restart identity cascade;
truncate table palikka_role_privilege restart identity cascade;