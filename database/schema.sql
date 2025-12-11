-- public.device definition

-- Drop table

-- DROP TABLE device;

CREATE TABLE device (
	id bigserial NOT NULL,
	always_on bool NOT NULL,
	first_seen timestamp NOT NULL,
	ip_address varchar(45) NULL,
	last_seen timestamp NOT NULL,
	mac_address varchar(17) NOT NULL,
	online bool NOT NULL,
	network_id int8 NOT NULL,
	allowed bool NULL,
	active_alarm_time timestamp NULL,
	CONSTRAINT device_pkey PRIMARY KEY (id),
	CONSTRAINT u_device_mac_address UNIQUE (mac_address)
);
CREATE INDEX i_device_network ON public.device USING btree (network_id);
CREATE INDEX idx_device_mac ON public.device USING btree (mac_address);


-- public.network definition

-- Drop table

-- DROP TABLE network;

CREATE TABLE network (
	id bigserial NOT NULL,
	"name" varchar(100) NOT NULL,
	first_seen timestamp NOT NULL,
	last_seen timestamp NOT NULL,
	alerting_delay int4 DEFAULT 300 NULL,
	CONSTRAINT network_name_key UNIQUE (name),
	CONSTRAINT network_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_network_name ON public.network USING btree (name);


-- public.device_status_history definition

-- Drop table

-- DROP TABLE device_status_history;

CREATE TABLE device_status_history (
	id bigserial NOT NULL,
	network_id int8 NOT NULL,
	mac_address varchar(17) NOT NULL,
	ip_address varchar(45) NOT NULL,
	online bool NOT NULL,
	"timestamp" timestamp NOT NULL,
	CONSTRAINT device_status_history_pkey PRIMARY KEY (id),
	CONSTRAINT fk_network FOREIGN KEY (network_id) REFERENCES network(id) ON DELETE CASCADE
);
CREATE INDEX idx_mac_timestamp ON public.device_status_history USING btree (mac_address, "timestamp" DESC);
CREATE INDEX idx_network_id ON public.device_status_history USING btree (network_id);
CREATE INDEX idx_network_mac_timestamp ON public.device_status_history USING btree (network_id, mac_address, "timestamp" DESC);