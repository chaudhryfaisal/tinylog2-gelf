package com.github.chaudhryfaisal.tinylog.gelf;

import org.tinylog.runtime.Timestamp;

import java.time.Instant;
import java.util.Date;

final class LegacyTimestamp implements Timestamp {

    private final Date date;

    /** */
    LegacyTimestamp() {
        date = new Date();
    }

    @Override
    public Date toDate() {
        return date;
    }

    @Override
    public Instant toInstant() {
        return date.toInstant();
    }

    @Override
    public java.sql.Timestamp toSqlTimestamp() {
        return new java.sql.Timestamp(date.getTime());
    }

}
