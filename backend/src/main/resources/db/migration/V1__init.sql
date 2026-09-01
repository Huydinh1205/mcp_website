CREATE TABLE [users] (
  national_id NVARCHAR(64)  NOT NULL PRIMARY KEY,
  first_name  NVARCHAR(255) NOT NULL,
  last_name   NVARCHAR(255) NOT NULL,
  email       NVARCHAR(320) NOT NULL
);

CREATE TABLE sellers (
  national_id NVARCHAR(64) NOT NULL PRIMARY KEY,
  rating      FLOAT        NOT NULL DEFAULT 0
);

CREATE TABLE buyers (
  national_id NVARCHAR(64)   NOT NULL PRIMARY KEY,
  interest    NVARCHAR(1000) NULL
);

CREATE TABLE products (
  product_id NVARCHAR(64)  NOT NULL PRIMARY KEY,
  name       NVARCHAR(255) NOT NULL,
  price      FLOAT         NOT NULL,
  min_price  FLOAT         NOT NULL,
  gap        FLOAT         NOT NULL DEFAULT 0,
  remainings INT           NOT NULL DEFAULT 1,
  seller_id  NVARCHAR(64)  NOT NULL
);

CREATE TABLE seller_ai_configs (
  agent_id          NVARCHAR(64) NOT NULL PRIMARY KEY,
  auto_accept_price FLOAT        NOT NULL,
  max_discount_step FLOAT        NOT NULL,
  product_id        NVARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE buyer_ai_configs (
  buyer_agent_id    NVARCHAR(64) NOT NULL PRIMARY KEY,
  max_budget        FLOAT        NOT NULL,
  target_price      FLOAT        NOT NULL,
  min_seller_rating FLOAT        NOT NULL DEFAULT 0,
  is_active         BIT          NOT NULL DEFAULT 1,
  style             NVARCHAR(50) NOT NULL DEFAULT 'fair',
  national_id       NVARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE negotiations (
  negotiation_id NVARCHAR(64) NOT NULL PRIMARY KEY,
  status         NVARCHAR(30) NOT NULL DEFAULT 'open',
  last_actor     NVARCHAR(10) NULL,
  current_round  INT          NOT NULL DEFAULT 0,
  current_price  FLOAT        NOT NULL DEFAULT 0,
  quantity       INT          NOT NULL DEFAULT 1,
  date           DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
  final_price    FLOAT        NULL,
  updated_at     DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
  national_id    NVARCHAR(64) NOT NULL,
  product_id     NVARCHAR(64) NOT NULL,
  order_id       NVARCHAR(64) NULL
);

CREATE TABLE negotiation_rounds (
  negotiation_id  NVARCHAR(64)   NOT NULL,
  round_number    INT            NOT NULL,
  proposed_price  FLOAT          NOT NULL,
  message_context NVARCHAR(2000) NOT NULL,
  author          NVARCHAR(10)   NOT NULL,
  CONSTRAINT pk_negotiation_rounds PRIMARY KEY (negotiation_id, round_number)
);

CREATE TABLE [orders] (
  order_id            NVARCHAR(64) NOT NULL PRIMARY KEY,
  status              NVARCHAR(20) NOT NULL DEFAULT 'pending',
  order_date          DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),
  buyer_confirmed_at  DATETIME2    NULL,
  seller_confirmed_at DATETIME2    NULL
);

CREATE INDEX ix_negotiations_national ON negotiations(national_id);
CREATE INDEX ix_negotiations_product  ON negotiations(product_id);
CREATE INDEX ix_products_seller       ON products(seller_id);
