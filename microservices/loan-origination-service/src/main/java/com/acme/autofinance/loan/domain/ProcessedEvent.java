package com.acme.autofinance.loan.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Inbox record used to make event consumption idempotent. A row is written the
 * first time a given event id is handled; subsequent deliveries of the same id
 * are ignored so a dealer submission never creates a second loan application and
 * lifecycle projections are never applied twice.
 */
@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    @Column(name = "event_key")
    private String eventKey;

    public ProcessedEvent() {}

    public ProcessedEvent(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
}
