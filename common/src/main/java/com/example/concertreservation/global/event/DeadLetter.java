package com.example.concertreservation.global.event;

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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE dead_letter SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "dead_letter")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class DeadLetter extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String uuid;

    private Long domainEventId;

    private String failReason;

    private boolean recovered;

    public DeadLetter(String uuid, Long domainEventId, String failReason) {
        this.uuid = uuid;
        this.domainEventId = domainEventId;
        this.failReason = failReason;
        this.recovered = false;
    }
}
