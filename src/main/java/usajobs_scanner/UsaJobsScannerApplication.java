package usajobs_scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import usajobs_scanner.service.JobSearchService;

import java.util.Scanner;

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
		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter keywords separated by spaces: ");
		String[] keywords = scanner.nextLine().toLowerCase().split("\\s+");

		System.out.println("How many recent days should vacancies be shown? ");
		int days = Integer.parseInt(scanner.nextLine());
		service.searchJobs(keywords, days);
		System.exit(0);
	}
}
