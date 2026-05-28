package com.example.concertreservation.global.event;

import com.example.concertreservation.global.domain.SoftDeletedDomain;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE domain_event SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "event_type")
@Table(name = "domain_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public abstract class DomainEvent extends SoftDeletedDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uuid;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    @Column(nullable = false)
    private Long targetDomainId;

    private String failReason;

    protected DomainEvent(Long targetDomainId) {
        this.uuid = UUID.randomUUID().toString();
        this.status = EventStatus.INIT;
        this.targetDomainId = targetDomainId;
    }

    public void regenerateUuid() {
        this.uuid = UUID.randomUUID().toString();
    }

    public void produceSuccess() {
        this.status = EventStatus.PRODUCE_SUCCESS;
    }

    public void produceFail(Throwable e) {
        this.status = EventStatus.PRODUCE_FAIL;
        this.failReason = e.getMessage();
    }

    public abstract String getTopic();
}
