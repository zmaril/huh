create table if not exists huh_base(
       pid int,
       time timestamp,
       instruction varchar(16),
       command varchar(20),
       args json,
       return json,
       return_msg varchar(100),
       total_runtime interval);
