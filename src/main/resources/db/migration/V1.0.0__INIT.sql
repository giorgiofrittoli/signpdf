--liquibase formatted sql
--changeset frigir:1.0.0
create table customer_sign (
    id bigint not null,
    name varchar(255),
    token varchar(255),
    primary key (id)
) engine=InnoDB;
create table customer_sign_SEQ (
    next_val bigint
) engine=InnoDB;
insert into customer_sign_SEQ values (1);

create table pdf_signed (id bigint not null, caller varchar(255), cellulare_otp varchar(255), firmatario varchar(255), md5 varchar(255), ts_firma varchar(255), uuid_firma varchar(255), customerSign_id bigint, primary key (id)) engine=InnoDB;
create table pdf_signed_SEQ (next_val bigint) engine=InnoDB;
insert into pdf_signed_SEQ values ( 1 );
alter table pdf_signed add constraint FKpxu1uwhluvkdco3j7c8eh0vsw foreign key (customerSign_id) references customer_sign (id);
