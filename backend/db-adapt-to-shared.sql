/* Adapt the shared schema (MCP_Ecommerce_AI_Agent) to this app.
   ADDITIVE ONLY - no drops, no type changes. Safe for existing data and for
   the other app on the same DB. Idempotent. Batches separated by GO. */

IF COL_LENGTH('dbo.[User]','Role') IS NULL ALTER TABLE dbo.[User] ADD [Role] varchar(20) NULL
GO
IF COL_LENGTH('dbo.Product','Category') IS NULL ALTER TABLE dbo.Product ADD Category nvarchar(50) NULL
GO
IF COL_LENGTH('dbo.Product','Shipping_Cost') IS NULL ALTER TABLE dbo.Product ADD Shipping_Cost decimal(18,2) NOT NULL DEFAULT 5
GO
IF COL_LENGTH('dbo.Product','Compare_At_Price') IS NULL ALTER TABLE dbo.Product ADD Compare_At_Price decimal(18,2) NULL
GO
IF COL_LENGTH('dbo.Product','Rating_Avg') IS NULL ALTER TABLE dbo.Product ADD Rating_Avg decimal(3,2) NOT NULL DEFAULT 0
GO
IF COL_LENGTH('dbo.Product','Rating_Count') IS NULL ALTER TABLE dbo.Product ADD Rating_Count int NOT NULL DEFAULT 0
GO
IF COL_LENGTH('dbo.Product','Sold_Count') IS NULL ALTER TABLE dbo.Product ADD Sold_Count int NOT NULL DEFAULT 0
GO
IF COL_LENGTH('dbo.Negotiation','Current_Price') IS NULL ALTER TABLE dbo.Negotiation ADD Current_Price decimal(18,2) NOT NULL DEFAULT 0
GO
IF COL_LENGTH('dbo.Negotiation','Current_Freebies_Cost') IS NULL ALTER TABLE dbo.Negotiation ADD Current_Freebies_Cost decimal(18,2) NOT NULL DEFAULT 0
GO
IF COL_LENGTH('dbo.Negotiation','Current_Free_Shipping') IS NULL ALTER TABLE dbo.Negotiation ADD Current_Free_Shipping bit NOT NULL DEFAULT 0
GO
IF COL_LENGTH('dbo.Negotiation_Round','Terms') IS NULL ALTER TABLE dbo.Negotiation_Round ADD Terms nvarchar(max) NULL
GO
IF COL_LENGTH('dbo.[Order]','Buyer_Confirmed_At') IS NULL ALTER TABLE dbo.[Order] ADD Buyer_Confirmed_At datetime2 NULL
GO
IF COL_LENGTH('dbo.[Order]','Seller_Confirmed_At') IS NULL ALTER TABLE dbo.[Order] ADD Seller_Confirmed_At datetime2 NULL
GO
IF OBJECT_ID('dbo.App_Coupon','U') IS NULL CREATE TABLE dbo.App_Coupon (Coupon_ID bigint IDENTITY(1,1) PRIMARY KEY, Code varchar(40) NOT NULL UNIQUE, Label nvarchar(120) NULL, Percent_Off decimal(5,4) NULL, Amount decimal(18,2) NULL, Product_ID bigint NULL, Seller_ID bigint NULL, Start_Date datetime2 NOT NULL DEFAULT sysutcdatetime(), End_Date datetime2 NOT NULL DEFAULT '2999-01-01')
GO
IF OBJECT_ID('dbo.App_Applied_Coupon','U') IS NULL CREATE TABLE dbo.App_Applied_Coupon (Negotiation_ID bigint NOT NULL, Coupon_ID bigint NOT NULL, Applied_At datetime2 NOT NULL DEFAULT sysutcdatetime(), CONSTRAINT PK_App_Applied_Coupon PRIMARY KEY (Negotiation_ID, Coupon_ID))
GO
IF OBJECT_ID('dbo.App_Review','U') IS NULL CREATE TABLE dbo.App_Review (Review_ID bigint IDENTITY(1,1) PRIMARY KEY, Product_ID bigint NOT NULL, Negotiation_ID bigint NOT NULL, Buyer_ID bigint NOT NULL, Rating_Score int NOT NULL, Comment nvarchar(1000) NULL, Reviewer_Name nvarchar(120) NULL, Verified bit NOT NULL DEFAULT 1, Created_At datetime2 NOT NULL DEFAULT sysutcdatetime())
GO
MERGE dbo.[User] AS u USING (VALUES (900001,N'Mai',N'Buyer','mai.demo@example.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','buyer'),(900002,N'Long',N'Buyer','long.demo@example.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','buyer'),(900003,N'KeyLab',N'Seller','keylab.demo@example.com','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','seller')) AS s(User_ID,First_Name,Last_Name,Email,Password_Hash,Role) ON u.User_ID=s.User_ID WHEN NOT MATCHED THEN INSERT (User_ID,First_Name,Last_Name,Email,Password_Hash,[Role]) VALUES (s.User_ID,s.First_Name,s.Last_Name,s.Email,s.Password_Hash,s.Role)
GO
IF NOT EXISTS (SELECT 1 FROM dbo.Buyer WHERE User_ID=900001) INSERT dbo.Buyer (User_ID,Interest) VALUES (900001,N'mechanical keyboards')
GO
IF NOT EXISTS (SELECT 1 FROM dbo.Buyer WHERE User_ID=900002) INSERT dbo.Buyer (User_ID,Interest) VALUES (900002,N'audio')
GO
IF NOT EXISTS (SELECT 1 FROM dbo.Seller WHERE User_ID=900003) INSERT dbo.Seller (User_ID,ABN,Trading_Name,Rating,Total_Ratings) VALUES (900003,'12345678901',N'KeyLab Store',4.7,120)
GO
IF NOT EXISTS (SELECT 1 FROM dbo.Buyer_AI_Config WHERE Buyer_ID=900001) INSERT dbo.Buyer_AI_Config (Buyer_Agent_ID,Buyer_ID,Max_Budget,Target_Price,Min_Seller_Rating,Style) VALUES (900001,900001,200,120,0,'MODERATE')
GO
IF NOT EXISTS (SELECT 1 FROM dbo.Buyer_AI_Config WHERE Buyer_ID=900002) INSERT dbo.Buyer_AI_Config (Buyer_Agent_ID,Buyer_ID,Max_Budget,Target_Price,Min_Seller_Rating,Style) VALUES (900002,900002,200,120,0,'MODERATE')
GO
