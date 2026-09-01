package com.marketplace;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProps(
    String confirmSecret,
    String sellerMode,
    String openaiApiKey,
    String openaiModel,
    String corsOrigin) {}
