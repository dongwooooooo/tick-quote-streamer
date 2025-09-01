package org.example.collector;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CollectorApplication {

	public static void main(String[] args) {
		// .env 파일 로드
		loadEnvironmentVariables();
		
		SpringApplication.run(CollectorApplication.class, args);
	}
	
	private static void loadEnvironmentVariables() {
		try {
			// 프로젝트 루트의 .env 파일 로드
			Dotenv dotenv = Dotenv.configure()
				.directory("../") // collector 디렉토리에서 한 단계 위로
				.ignoreIfMissing() // .env 파일이 없어도 오류 안남
				.load();
			
			// 환경변수를 시스템 프로퍼티로 설정
			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
				if (entry.getKey().contains("KIS_APP")) {
					System.out.println(entry.getKey() + ": " + entry.getValue().substring(0, 10) + "...");
				} else {
					System.out.println(entry.getKey() + ": " + entry.getValue());
				}
			});
			
			System.out.println(".env 파일 로드 완료");
		} catch (Exception e) {
			System.out.println(".env 파일 로드 실패 (하지만 계속 진행): " + e.getMessage());
		}
	}
	

}
