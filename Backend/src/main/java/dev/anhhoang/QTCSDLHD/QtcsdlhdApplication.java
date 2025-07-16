package dev.anhhoang.QTCSDLHD;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QtcsdlhdApplication  {

	// Inject a value from your .env file


	public static void main(String[] args) {
		SpringApplication.run(QtcsdlhdApplication.class, args);
	}

	// This method will run after the application starts


}
