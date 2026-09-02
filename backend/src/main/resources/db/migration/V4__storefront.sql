-- Storefront realism: compare-at price, aggregate rating, sold count; reviewer name.
ALTER TABLE products ADD compare_at_price FLOAT NULL;
ALTER TABLE products ADD rating_avg FLOAT NOT NULL DEFAULT 0;
ALTER TABLE products ADD rating_count INT NOT NULL DEFAULT 0;
ALTER TABLE products ADD sold_count INT NOT NULL DEFAULT 0;

ALTER TABLE feedback ADD reviewer_name NVARCHAR(120) NULL;
ALTER TABLE feedback ADD verified BIT NOT NULL DEFAULT 1;
