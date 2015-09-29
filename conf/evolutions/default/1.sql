# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table cluster (
  id                        integer auto_increment not null,
  parent_id                 integer,
  constraint pk_cluster primary key (id))
;

create table commodity (
  id                        bigint auto_increment not null,
  cluster_id                integer not null,
  startLatitude             double not null,
  startLongitude            double not null,
  endLatitude               double not null,
  endLongitude              double not null,
  param                     integer,
  constraint pk_commodity primary key (id))
;

create table vehicle (
  id                        bigint auto_increment not null,
  cluster_id                integer not null,
  latitude                  double not null,
  longitude                 double not null,
  capacity                  integer not null,
  constraint pk_vehicle primary key (id))
;

alter table cluster add constraint fk_cluster_parent_1 foreign key (parent_id) references cluster (id) on delete restrict on update restrict;
create index ix_cluster_parent_1 on cluster (parent_id);
alter table commodity add constraint fk_commodity_cluster_2 foreign key (cluster_id) references cluster (id) on delete restrict on update restrict;
create index ix_commodity_cluster_2 on commodity (cluster_id);
alter table vehicle add constraint fk_vehicle_cluster_3 foreign key (cluster_id) references cluster (id) on delete restrict on update restrict;
create index ix_vehicle_cluster_3 on vehicle (cluster_id);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists cluster;

drop table if exists commodity;

drop table if exists vehicle;

SET REFERENTIAL_INTEGRITY TRUE;

