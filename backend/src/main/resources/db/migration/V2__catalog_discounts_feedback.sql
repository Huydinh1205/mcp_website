-- Catalog category, seller coupons, and post-purchase reviews.
-- applyTurn() is unchanged: coupons apply at confirmation, not per turn.

ALTER TABLE products ADD category NVARCHAR(50) NULL;
ALTER TABLE products ADD shipping_cost FLOAT NOT NULL DEFAULT 5;

CREATE TABLE discounts (
  discount_id NVARCHAR(64)  NOT NULL PRIMARY KEY,
  code        NVARCHAR(40)  NOT NULL UNIQUE,
  label       NVARCHAR(120) NULL,
  percent_off FLOAT         NULL,   -- 0..1 (e.g. 0.10 = 10% off)
  amount      FLOAT         NULL,   -- flat amount off (use one of percent/amount)
  product_id  NVARCHAR(64)  NULL,   -- null + seller_id set  -> any product from that seller
  seller_id   NVARCHAR(64)  NULL,   -- both null             -> global coupon
  start_date  DATETIME2     NOT NULL,
  end_date    DATETIME2     NOT NULL
);

CREATE TABLE applied_discounts (
  negotiation_id NVARCHAR(64) NOT NULL,
  discount_id    NVARCHAR(64) NOT NULL,
  applied_at     DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT pk_applied_discounts PRIMARY KEY (negotiation_id, discount_id)
);

CREATE TABLE feedback (
  feedback_id    NVARCHAR(64)   NOT NULL PRIMARY KEY,
  product_id     NVARCHAR(64)   NOT NULL,
  negotiation_id NVARCHAR(64)   NOT NULL,
  buyer_id       NVARCHAR(64)   NOT NULL,
  rating_score   INT            NOT NULL,
  comment        NVARCHAR(1000) NULL,
  created_at     DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME()
);

CREATE INDEX ix_discounts_product ON discounts(product_id);
CREATE INDEX ix_discounts_seller  ON discounts(seller_id);
CREATE INDEX ix_feedback_product  ON feedback(product_id);
CREATE INDEX ix_products_category ON products(category);

-- Multi-term deal state (freebies / free shipping) carried on the negotiation.
ALTER TABLE negotiations ADD current_freebies_cost FLOAT NOT NULL DEFAULT 0;
ALTER TABLE negotiations ADD current_free_shipping BIT NOT NULL DEFAULT 0;
ALTER TABLE negotiation_rounds ADD terms NVARCHAR(MAX) NULL;
