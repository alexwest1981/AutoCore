package com.wac.autocore.config;

import com.wac.autocore.data.Db;
import com.wac.autocore.repository.RepositoryFactory;
import com.wac.autocore.repository.SqliteRepositoryFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The storage, chosen once by the configuration and handed out as a bean.
 *
 * The path is a property so that a run against a copy of the real database is a
 * setting and not a line in the code: {@code -Dautocore.db.path=/tmp/kopia.db}.
 */
@Configuration
public class DatabaseConfig {

    @Bean
    public Db db(@Value("${autocore.db.path:" + Db.DEFAULT_PATH + "}") String path) {
        return new Db(path);
    }

    @Bean
    public RepositoryFactory repositoryFactory(Db db) {
        return new SqliteRepositoryFactory(db);
    }
}
