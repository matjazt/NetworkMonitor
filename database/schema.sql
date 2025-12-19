-- public.account_type definition

-- Drop table

-- DROP TABLE account_type;

CREATE TABLE account_type (
	id bigserial NOT NULL,
	description varchar(255) NULL,
	"name" varchar(50) NOT NULL,
	CONSTRAINT account_type_pkey PRIMARY KEY (id),
	CONSTRAINT u_ccnttyp_name UNIQUE (name)
);


-- public.alert_type definition

-- Drop table

-- DROP TABLE alert_type;

CREATE TABLE alert_type (
	id int4 NOT NULL,
	description varchar(255) NULL,
	"name" varchar(50) NOT NULL,
	CONSTRAINT alert_type_pkey PRIMARY KEY (id),
	CONSTRAINT u_lrt_typ_name UNIQUE (name)
);


-- public.device_operation_mode definition

-- Drop table

-- DROP TABLE device_operation_mode;

CREATE TABLE device_operation_mode (
	id int4 NOT NULL,
	"name" varchar(50) NOT NULL,
	description text NULL,
	CONSTRAINT device_operation_mode_name_key UNIQUE (name),
	CONSTRAINT device_operation_mode_pkey PRIMARY KEY (id)
);


-- public.device_status_history definition

-- Drop table

-- DROP TABLE device_status_history;

CREATE TABLE device_status_history (
	id bigserial NOT NULL,
	ip_address varchar(45) NOT NULL,
	online bool NOT NULL,
	"timestamp" timestamp NOT NULL,
	network_id int8 NOT NULL,
	device_id int8 NULL,
	CONSTRAINT device_status_history_pkey PRIMARY KEY (id)
);


-- public.network definition

-- Drop table

-- DROP TABLE network;

CREATE TABLE network (
	id bigserial NOT NULL,
	alerting_delay int4 DEFAULT 300 NOT NULL,
	email_address varchar(1000) NULL,
	first_seen timestamp NOT NULL,
	last_seen timestamp NOT NULL,
	"name" varchar(100) NOT NULL,
	active_alert_id int8 NULL,
	CONSTRAINT network_pkey PRIMARY KEY (id),
	CONSTRAINT u_network_name UNIQUE (name)
);


-- public.account definition

-- Drop table

-- DROP TABLE account;

CREATE TABLE account (
	id bigserial NOT NULL,
	created_at timestamp NOT NULL,
	email varchar(200) NOT NULL,
	full_name varchar(200) NOT NULL,
	last_seen timestamp NULL,
	password_hash varchar(255) NOT NULL,
	username varchar(100) NOT NULL,
	account_type_id int4 NOT NULL,
	CONSTRAINT account_pkey PRIMARY KEY (id),
	CONSTRAINT u_account_email UNIQUE (email),
	CONSTRAINT u_account_username UNIQUE (username),
	CONSTRAINT fk_account_account_type FOREIGN KEY (account_type_id) REFERENCES account_type(id)
);
CREATE INDEX i_account_accounttype ON public.account USING btree (account_type_id);


-- public.account_network definition

-- Drop table

-- DROP TABLE account_network;

CREATE TABLE account_network (
	id bigserial NOT NULL,
	account_id int8 NOT NULL,
	network_id int8 NOT NULL,
	CONSTRAINT account_network_pkey PRIMARY KEY (id),
	CONSTRAINT fk_account_network_account FOREIGN KEY (account_id) REFERENCES account(id),
	CONSTRAINT fk_account_network_network FOREIGN KEY (network_id) REFERENCES network(id)
);
CREATE INDEX i_ccntwrk_account ON public.account_network USING btree (account_id);
CREATE INDEX i_ccntwrk_network ON public.account_network USING btree (network_id);


-- public.device definition

-- Drop table

-- DROP TABLE device;

CREATE TABLE device (
	id bigserial NOT NULL,
	first_seen timestamp NOT NULL,
	ip_address varchar(45) NULL,
	last_seen timestamp NOT NULL,
	mac_address varchar(17) NOT NULL,
	"name" varchar(200) NULL,
	online bool NOT NULL,
	network_id int8 NOT NULL,
	device_operation_mode_id int4 NOT NULL,
	active_alert_id int8 NULL,
	CONSTRAINT device_pkey PRIMARY KEY (id),
	CONSTRAINT fk_device_device_operation_mode FOREIGN KEY (device_operation_mode_id) REFERENCES device_operation_mode(id),
	CONSTRAINT fk_devicet_network FOREIGN KEY (network_id) REFERENCES network(id)
);
CREATE INDEX i_device_network ON public.device USING btree (network_id);
CREATE INDEX idx_device_mac ON public.device USING btree (mac_address);


-- public.alert definition

-- Drop table

-- DROP TABLE alert;

CREATE TABLE alert (
	id bigserial NOT NULL,
	alert_type_id int2 NULL,
	closure_timestamp timestamp NULL,
	message varchar(500) NULL,
	"timestamp" timestamp NOT NULL,
	device_id int8 NULL,
	network_id int8 NOT NULL,
	CONSTRAINT alert_pkey PRIMARY KEY (id),
	CONSTRAINT fk_alert_alert_type FOREIGN KEY (alert_type_id) REFERENCES alert_type(id),
	CONSTRAINT fk_alert_device FOREIGN KEY (device_id) REFERENCES device(id),
	CONSTRAINT fk_alert_network FOREIGN KEY (network_id) REFERENCES network(id)
);
CREATE INDEX i_alert_alerttyperef ON public.alert USING btree (alert_type_id);
CREATE INDEX i_alert_device ON public.alert USING btree (device_id);
CREATE INDEX i_alert_network ON public.alert USING btree (network_id);
CREATE INDEX idx_alert_timestamp ON public.alert USING btree ("timestamp");



INSERT INTO alert_type (id, name, description) VALUES
    (0, 'NETWORK_DOWN', 'Network connectivity lost or network went offline'),
    (1, 'DEVICE_DOWN', 'Device that should always be online is not responding'),
    (2, 'DEVICE_NOT_ALLOWED', 'Unauthorized device detected on the network');


INSERT INTO device_operation_mode (id, name, description) VALUES
    (0, 'NOT_ALLOWED', 'Device is not allowed on the network'),
    (1, 'ALLOWED', 'Device is allowed but not monitored'),
    (2, 'ALWAYS_ON', 'Device should always be online and is monitored');

insert into account_type (id, name, description) values 
(1, 'admin', 'administrator'),
 (2, 'user', 'ordinary user'), 
 (3, 'device', 'monitoring device')
