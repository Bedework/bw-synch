drop table if exists bwsynch_subs cascade;

drop sequence if exists hibernate_sequence;
create sequence hibernate_sequence start 1 increment 1;

create table bwsynch_subs (
    bwsyn_id int8 not null,
    bwsyn_seq int4 not null,
    bwsyn_subid varchar(250) not null,
    bwsyn_owner varchar(500) not null,
    bwsyn_lrefresh varchar(20),
    bwsyn_errorct int4,
    bwsyn_missing char(1) not null,
    bwsyn_connectorid_a varchar(100),
    bwsyn_conn_props_a varchar(3000),
    bwsyn_connectorid_b varchar(100),
    bwsyn_conn_props_b varchar(3000),
    bwsyn_props varchar(3000),
    bwsyn_dir varchar(25) not null,
    bwsyn_mstr varchar(25) not null,
    primary key (bwsyn_id)
);

create index bwsynidx_subid on bwsynch_subs (bwsyn_subid);
create index bwsynidx_subowner on bwsynch_subs (bwsyn_owner);

alter table bwsynch_subs
    add constraint UK_qptomm2syatpqumsl1udwk7be unique (bwsyn_subid);
