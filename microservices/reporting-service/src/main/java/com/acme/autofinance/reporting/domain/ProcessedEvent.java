package com.acme.autofinance.reporting.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "reporting_processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    public ProcessedEvent() {}

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() { return eventId; }
}
