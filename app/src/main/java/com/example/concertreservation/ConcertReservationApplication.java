package com.example.concertreservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.example.concertreservation")
@EntityScan(basePackages = "com.example.concertreservation")
@EnableJpaRepositories(basePackages = "com.example.concertreservation")
@ConfigurationPropertiesScan
@EnableScheduling
public class ConcertReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcertReservationApplication.class, args);
    }

}
