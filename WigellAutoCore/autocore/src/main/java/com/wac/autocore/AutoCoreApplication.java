package com.wac.autocore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Spring starts here. The app owns its SQLite file, so the DataSource autoconfiguration is off.
@SpringBootApplication
public class AutoCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoCoreApplication.class, args);
    }
}
