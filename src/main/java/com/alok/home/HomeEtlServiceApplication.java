package com.alok.home;

import com.alok.home.commons.utils.annotation.LogExecutionTime;
import com.alok.home.service.JobExecutorOfBankService;
import com.alok.home.service.JobExecutorOfExpenseService;
import com.alok.home.service.JobExecutorOfInvestmentService;
import com.alok.home.service.JobExecutorOfTaxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan({
		"com.alok.spring.mqtt.config",
		"com.alok.spring.config",
		"com.alok.home.commons.security.properties"
})
@EnableScheduling
@SpringBootApplication(
		scanBasePackages = {
				"com.alok.home",
				"com.alok.home.commons.exception",
				"com.alok.home.commons.entity",
				"com.alok.home.commons.repository",
				"com.alok.home.commons.utils",
				"com.alok.home.commons.utils.annotations",
				"com.alok.home.commons.constant",
				"com.alok.home.commons.security"
		}
)
@Slf4j
public class HomeEtlServiceApplication implements ApplicationRunner {

	private final JobExecutorOfBankService jobExecutorOfBankService;
	private final JobExecutorOfExpenseService jobExecutorOfExpenseService;
	private final JobExecutorOfTaxService jobExecutorOfTaxService;
	private final JobExecutorOfInvestmentService jobExecutorOfInvestmentService;

	@Autowired
	public HomeEtlServiceApplication(
			JobExecutorOfBankService jobExecutorOfBankService, JobExecutorOfExpenseService jobExecutorOfExpenseService,
			JobExecutorOfTaxService jobExecutorOfTaxService, JobExecutorOfInvestmentService jobExecutorOfInvestmentService
	) {
		this.jobExecutorOfBankService = jobExecutorOfBankService;
		this.jobExecutorOfExpenseService = jobExecutorOfExpenseService;
		this.jobExecutorOfTaxService = jobExecutorOfTaxService;
		this.jobExecutorOfInvestmentService = jobExecutorOfInvestmentService;

	}

	public static void main(String[] args) {
		SpringApplication.run(HomeEtlServiceApplication.class, args);
	}

	@LogExecutionTime
	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Application Started!!!");
		System.out.println("Application Started!!!");

		jobExecutorOfBankService.executeAllBatchJobs();
		jobExecutorOfExpenseService.executeAllJobs();
		jobExecutorOfTaxService.executeAllJobs();
		jobExecutorOfInvestmentService.executeAllJobs();

		log.info("All jobs completed!!!");
		System.out.println("All jobs completed!!!");
	}
}
