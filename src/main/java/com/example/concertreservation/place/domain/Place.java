package com.example.concertreservation.place.domain;

import com.example.concertreservation.global.domain.SoftDeletedDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "place")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "name", nullable = false)
    private String placeName;

    @Column(name = "address", nullable = false)
    private String placeAddress;

    @Column(name = "seat_count", nullable = false)
    private Integer seatCount;

    public Place(String placeName, String placeAddress, Integer seatCount) {
        this.placeName = placeName;
        this.placeAddress = placeAddress;
        this.seatCount = seatCount;
    }
}
