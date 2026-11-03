package com.promptvault;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
@MapperScan("com.promptvault.mapper")
public class PromptVaultApplication {

    public static void main(String[] args) {
        // SQLite 不会自动创建父目录，启动前先保证数据目录存在
        new File("data").mkdirs();
        SpringApplication.run(PromptVaultApplication.class, args);
    }
}
