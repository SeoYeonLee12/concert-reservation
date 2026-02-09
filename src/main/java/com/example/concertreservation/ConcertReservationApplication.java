package com.example.concertreservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class ConcertReservationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConcertReservationApplication.class, args);
    }

}
