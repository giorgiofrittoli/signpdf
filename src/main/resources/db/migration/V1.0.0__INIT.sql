--liquibase formatted sql
--changeset frigir:1.0.0
create table customer_sign (
    id bigint not null,
    name varchar(255),
    token varchar(255),
    tot integer,
    primary key (id)
) engine=InnoDB;
create table customer_sign_SEQ (
    next_val bigint
) engine=InnoDB;
insert into customer_sign_SEQ values (1);
