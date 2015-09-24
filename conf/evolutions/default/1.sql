# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table commodity (
  id                        bigint auto_increment not null,
  startLatitude             double not null,
  startLongitude            double not null,
  endLatitude               double not null,
  endLongitude              double not null,
  param                     integer,
  constraint pk_commodity primary key (id))
;

create table vehicle (
  id                        bigint auto_increment not null,
  latitude                  double not null,
  longitude                 double not null,
  capacity                  integer not null,
  constraint pk_vehicle primary key (id))
;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists commodity;

drop table if exists vehicle;

SET REFERENTIAL_INTEGRITY TRUE;

