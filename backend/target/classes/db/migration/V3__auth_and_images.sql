ALTER TABLE ${app_schema}.users ADD role NVARCHAR(20) NULL;
ALTER TABLE ${app_schema}.users ADD password_hash NVARCHAR(200) NULL;
ALTER TABLE ${app_schema}.products ADD image_url NVARCHAR(500) NULL;
