package usajobs_scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import usajobs_scanner.service.JobSearchService;

@SpringBootApplication
public class UsaJobsScannerApplication implements CommandLineRunner {
	private final JobSearchService service;

	public UsaJobsScannerApplication(JobSearchService service) {
		this.service = service;
	}

	public static void main(String[] args) {
		SpringApplication.run(UsaJobsScannerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		service.searchJobs("java");
		System.exit(0);
	}
}
