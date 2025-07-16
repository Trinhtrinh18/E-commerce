package dev.anhhoang.QTCSDLHD.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // Kích hoạt auditing cho MongoDB
}