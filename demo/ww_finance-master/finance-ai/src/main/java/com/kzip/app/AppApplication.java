package com.kzip.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.vectorstore.chroma.autoconfigure.ChromaVectorStoreAutoConfiguration;

// 排除 Chroma 自动装配：VectorStore 已在 CommonConfiguration 里手动构造，
// 避免与自动配置类的同名 bean 冲突。
@SpringBootApplication(exclude = ChromaVectorStoreAutoConfiguration.class)
public class AppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}

}
