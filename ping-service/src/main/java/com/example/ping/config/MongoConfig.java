package com.example.ping.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.ping.mongo")
public class MongoConfig extends AbstractMongoClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/pingpong}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:pingpong}")
    private String databaseName;

    @Override
    public MongoClient mongoClient() {
        log.info("[MongoConfig] 初始化 MongoDB 连接, uri={}, database={}", mongoUri, databaseName);
        
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        
        log.info("[MongoConfig] ✓ MongoDB 客户端创建成功");
        return MongoClients.create(mongoClientSettings);
    }

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }
}
