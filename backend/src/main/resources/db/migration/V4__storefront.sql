-- Storefront realism: compare-at price, aggregate rating, sold count; reviewer name.
ALTER TABLE ${app_schema}.products ADD compare_at_price FLOAT NULL;
ALTER TABLE ${app_schema}.products ADD rating_avg FLOAT NOT NULL DEFAULT 0;
ALTER TABLE ${app_schema}.products ADD rating_count INT NOT NULL DEFAULT 0;
ALTER TABLE ${app_schema}.products ADD sold_count INT NOT NULL DEFAULT 0;

ALTER TABLE ${app_schema}.feedback ADD reviewer_name NVARCHAR(120) NULL;
ALTER TABLE ${app_schema}.feedback ADD verified BIT NOT NULL DEFAULT 1;
