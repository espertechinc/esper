// Cut & paste the below into the Oracle

drop table "test"."mytesttable";

create table "test"."mytesttable"(
	mybigint number(38, 0),
	myint integer,
	myvarchar varchar2(20),
	mychar char(20),
	mybool char(1),
	mynumeric numeric(8),
	mydecimal numeric(8),
	mydouble double precision,	
	myreal float
);

create unique index mytesttable_index_1 on "test"."mytesttable"(mybigint);

insert into "test"."mytesttable" values (1, 10, 'A', 'Z', 'T', 5000, 100, 1.2, 1.3);
insert into "test"."mytesttable" values (2, 20, 'B', 'Y', 'T', 100, 200, 2.2, 2.3);
insert into "test"."mytesttable" values (3, 30, 'C', 'X', 'T', 100, 300, 3.2, 3.3);
insert into "test"."mytesttable" values (4, 40, 'D', 'W', 'T', 500, 400, 4.2, 4.3);
insert into "test"."mytesttable" values (5, 50, 'E', 'V', 'T', 500, 500, 5.2, 5.3);
insert into "test"."mytesttable" values (6, 60, 'F', 'T', 'T', 200, 600, 6.2, 6.3);
insert into "test"."mytesttable" values (7, 70, 'G', 'S', 'T', null, 700, 7.2, 7.3);
insert into "test"."mytesttable" values (8, 80, 'H', 'R', 'T', null, 800, 8.2, 8.3);
insert into "test"."mytesttable" values (9, 90, 'I', 'Q', 'T', null, 900, 9.2, 9.3);
insert into "test"."mytesttable" values (10, 100,  'J', 'P', 'T', null, 1000, 10.2, 10.3);

	
