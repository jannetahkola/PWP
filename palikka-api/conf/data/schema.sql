--
-- PostgreSQL database dump
--

-- Dumped from database version 16.2 (Debian 16.2-1.pgdg120+2)
-- Dumped by pg_dump version 16.2 (Debian 16.2-1.pgdg120+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: palikka_privilege; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.palikka_privilege (
    id integer NOT NULL,
    domain character varying(255) NOT NULL,
    domain_description character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.palikka_privilege OWNER TO "user";

--
-- Name: palikka_privilege_id_seq; Type: SEQUENCE; Schema: public; Owner: user
--

CREATE SEQUENCE public.palikka_privilege_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.palikka_privilege_id_seq OWNER TO "user";

--
-- Name: palikka_privilege_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: user
--

ALTER SEQUENCE public.palikka_privilege_id_seq OWNED BY public.palikka_privilege.id;


--
-- Name: palikka_resource_security_rule; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.palikka_resource_security_rule (
    id integer NOT NULL,
    allow_system boolean NOT NULL,
    operation character varying(255) NOT NULL
);


ALTER TABLE public.palikka_resource_security_rule OWNER TO "user";

--
-- Name: palikka_resource_security_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: user
--

CREATE SEQUENCE public.palikka_resource_security_rule_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.palikka_resource_security_rule_id_seq OWNER TO "user";

--
-- Name: palikka_resource_security_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: user
--

ALTER SEQUENCE public.palikka_resource_security_rule_id_seq OWNED BY public.palikka_resource_security_rule.id;


--
-- Name: palikka_resource_security_rule_role; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.palikka_resource_security_rule_role (
    resource_security_rule_id integer NOT NULL,
    role_id integer NOT NULL
);


ALTER TABLE public.palikka_resource_security_rule_role OWNER TO "user";

--
-- Name: palikka_role; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.palikka_role (
    id integer NOT NULL,
    description character varying(255),
    name character varying(255) NOT NULL
);


ALTER TABLE public.palikka_role OWNER TO "user";

--
-- Name: palikka_role_id_seq; Type: SEQUENCE; Schema: public; Owner: user
--

CREATE SEQUENCE public.palikka_role_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.palikka_role_id_seq OWNER TO "user";

--
-- Name: palikka_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: user
--

ALTER SEQUENCE public.palikka_role_id_seq OWNED BY public.palikka_role.id;


--
-- Name: palikka_role_privilege; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.palikka_role_privilege (
    role_id integer NOT NULL,
    privilege_id integer NOT NULL
);


ALTER TABLE public.palikka_role_privilege OWNER TO "user";

--
-- Name: palikka_user; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.palikka_user (
    id integer NOT NULL,
    active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_updated_at timestamp without time zone,
    password character varying(255) NOT NULL,
    root boolean DEFAULT false NOT NULL,
    salt character varying(255) NOT NULL,
    username character varying(255) NOT NULL
);


ALTER TABLE public.palikka_user OWNER TO "user";

--
-- Name: palikka_user_id_seq; Type: SEQUENCE; Schema: public; Owner: user
--

CREATE SEQUENCE public.palikka_user_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.palikka_user_id_seq OWNER TO "user";

--
-- Name: palikka_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: user
--

ALTER SEQUENCE public.palikka_user_id_seq OWNED BY public.palikka_user.id;


--
-- Name: palikka_user_role; Type: TABLE; Schema: public; Owner: user
--

CREATE TABLE public.palikka_user_role (
    user_id integer NOT NULL,
    role_id integer NOT NULL
);


ALTER TABLE public.palikka_user_role OWNER TO "user";

--
-- Name: palikka_privilege id; Type: DEFAULT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_privilege ALTER COLUMN id SET DEFAULT nextval('public.palikka_privilege_id_seq'::regclass);


--
-- Name: palikka_resource_security_rule id; Type: DEFAULT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_resource_security_rule ALTER COLUMN id SET DEFAULT nextval('public.palikka_resource_security_rule_id_seq'::regclass);


--
-- Name: palikka_role id; Type: DEFAULT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_role ALTER COLUMN id SET DEFAULT nextval('public.palikka_role_id_seq'::regclass);


--
-- Name: palikka_user id; Type: DEFAULT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_user ALTER COLUMN id SET DEFAULT nextval('public.palikka_user_id_seq'::regclass);


--
-- Name: palikka_privilege idx_unique_domain_name; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_privilege
    ADD CONSTRAINT idx_unique_domain_name UNIQUE (domain, name);


--
-- Name: palikka_privilege palikka_privilege_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_privilege
    ADD CONSTRAINT palikka_privilege_pkey PRIMARY KEY (id);


--
-- Name: palikka_resource_security_rule palikka_resource_security_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_resource_security_rule
    ADD CONSTRAINT palikka_resource_security_rule_pkey PRIMARY KEY (id);


--
-- Name: palikka_resource_security_rule_role palikka_resource_security_rule_role_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_resource_security_rule_role
    ADD CONSTRAINT palikka_resource_security_rule_role_pkey PRIMARY KEY (resource_security_rule_id, role_id);


--
-- Name: palikka_role palikka_role_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_role
    ADD CONSTRAINT palikka_role_pkey PRIMARY KEY (id);


--
-- Name: palikka_role_privilege palikka_role_privilege_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_role_privilege
    ADD CONSTRAINT palikka_role_privilege_pkey PRIMARY KEY (role_id, privilege_id);


--
-- Name: palikka_user palikka_user_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_user
    ADD CONSTRAINT palikka_user_pkey PRIMARY KEY (id);


--
-- Name: palikka_user_role palikka_user_role_pkey; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_user_role
    ADD CONSTRAINT palikka_user_role_pkey PRIMARY KEY (user_id, role_id);


--
-- Name: palikka_user uk_8tl1l2cp4dsvt71ojvxkdeir8; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_user
    ADD CONSTRAINT uk_8tl1l2cp4dsvt71ojvxkdeir8 UNIQUE (username);


--
-- Name: palikka_role uk_get8fnqpeo5gplquvp3apiyd2; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_role
    ADD CONSTRAINT uk_get8fnqpeo5gplquvp3apiyd2 UNIQUE (name);


--
-- Name: palikka_resource_security_rule uk_os8ofenjo5vlecvyjimdwbrk7; Type: CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_resource_security_rule
    ADD CONSTRAINT uk_os8ofenjo5vlecvyjimdwbrk7 UNIQUE (operation);


--
-- Name: palikka_user_role fk15j0dk4mvgjhud6jpk0a37l3e; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_user_role
    ADD CONSTRAINT fk15j0dk4mvgjhud6jpk0a37l3e FOREIGN KEY (role_id) REFERENCES public.palikka_role(id);


--
-- Name: palikka_role_privilege fk3lkwklim8birobeosrr9ihvj7; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_role_privilege
    ADD CONSTRAINT fk3lkwklim8birobeosrr9ihvj7 FOREIGN KEY (privilege_id) REFERENCES public.palikka_privilege(id);


--
-- Name: palikka_resource_security_rule_role fk4hjx5tm5rd3urnsedx204ikxl; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_resource_security_rule_role
    ADD CONSTRAINT fk4hjx5tm5rd3urnsedx204ikxl FOREIGN KEY (resource_security_rule_id) REFERENCES public.palikka_resource_security_rule(id);


--
-- Name: palikka_user_role fk6bkefmrqhcw0ae617e56kjo9a; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_user_role
    ADD CONSTRAINT fk6bkefmrqhcw0ae617e56kjo9a FOREIGN KEY (user_id) REFERENCES public.palikka_user(id);


--
-- Name: palikka_role_privilege fkd5s62l4xafrrl3rr2g0spl14o; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_role_privilege
    ADD CONSTRAINT fkd5s62l4xafrrl3rr2g0spl14o FOREIGN KEY (role_id) REFERENCES public.palikka_role(id);


--
-- Name: palikka_resource_security_rule_role fkdu1ydii5abiyg5381ohvbdfwq; Type: FK CONSTRAINT; Schema: public; Owner: user
--

ALTER TABLE ONLY public.palikka_resource_security_rule_role
    ADD CONSTRAINT fkdu1ydii5abiyg5381ohvbdfwq FOREIGN KEY (role_id) REFERENCES public.palikka_role(id);


--
-- PostgreSQL database dump complete
--

