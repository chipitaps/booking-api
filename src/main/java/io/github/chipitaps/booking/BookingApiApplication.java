package io.github.chipitaps.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingApiApplication.class, args);
	}

}
