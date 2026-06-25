ALTER TABLE assets ADD COLUMN recommended_price NUMERIC(12,2);
ALTER TABLE assets ADD COLUMN last_calculated_at TIMESTAMP;
