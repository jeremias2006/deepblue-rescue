ALTER TABLE animals ADD COLUMN tracking_device_code VARCHAR(50);
ALTER TABLE animals ADD CONSTRAINT uk_tracking_device UNIQUE (tracking_device_code);