// Copyright Verizon Media. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.

package com.yahoo.container.logging;

import com.yahoo.slime.Cursor;
import com.yahoo.slime.Slime;
import com.yahoo.slime.SlimeUtils;
import com.yahoo.slime.Type;
import com.yahoo.yolean.Exceptions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * @author mortent
 */
public class ConnectionLogEntry {

    private final UUID id;
    private final Instant timestamp;
    private final Double durationSeconds;
    private final String peerAddress;
    private final Integer peerPort;
    private final String localAddress;
    private final Integer localPort;
    private final String remoteAddress;
    private final Integer remotePort;
    private final Long httpBytesReceived;
    private final Long httpBytesSent;
    private final Long requests;
    private final Long responses;
    private final String sslSessionId;
    private final String sslProtocol;
    private final String sslCipherSuite;
    private final String sslPeerSubject;
    private final Instant sslPeerNotBefore;
    private final Instant sslPeerNotAfter;
    private final String sslSniServerName;
    private final String sslHandshakeFailureException;
    private final String sslHandshakeFailureMessage;
    private final String sslHandshakeFailureType;


    private ConnectionLogEntry(Builder builder) {
        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.durationSeconds = builder.durationSeconds;
        this.peerAddress = builder.peerAddress;
        this.peerPort = builder.peerPort;
        this.localAddress = builder.localAddress;
        this.localPort = builder.localPort;
        this.remoteAddress = builder.remoteAddress;
        this.remotePort = builder.remotePort;
        this.httpBytesReceived = builder.httpBytesReceived;
        this.httpBytesSent = builder.httpBytesSent;
        this.requests = builder.requests;
        this.responses = builder.responses;
        this.sslSessionId = builder.sslSessionId;
        this.sslProtocol = builder.sslProtocol;
        this.sslCipherSuite = builder.sslCipherSuite;
        this.sslPeerSubject = builder.sslPeerSubject;
        this.sslPeerNotBefore = builder.sslPeerNotBefore;
        this.sslPeerNotAfter = builder.sslPeerNotAfter;
        this.sslSniServerName = builder.sslSniServerName;
        this.sslHandshakeFailureException = builder.sslHandshakeFailureException;
        this.sslHandshakeFailureMessage = builder.sslHandshakeFailureMessage;
        this.sslHandshakeFailureType = builder.sslHandshakeFailureType;
    }

    public String toJson() {
        Slime slime = new Slime();
        Cursor cursor = slime.setObject();
        cursor.setString("id", id.toString());
        setTimestamp(cursor, timestamp, "timestamp");

        setDouble(cursor, durationSeconds, "duration");
        setString(cursor, peerAddress, "peerAddress");
        setInteger(cursor, peerPort, "peerPort");
        setString(cursor, localAddress, "localAddress");
        setInteger(cursor, localPort, "localPort");
        setString(cursor, remoteAddress, "remoteAddress");
        setInteger(cursor, remotePort, "remotePort");
        setLong(cursor, httpBytesReceived, "httpBytesReceived");
        setLong(cursor, httpBytesSent, "httpBytesSent");
        setLong(cursor, requests, "requests");
        setLong(cursor, responses, "responses");
        setString(cursor, sslProtocol, "ssl", "protocol");
        setString(cursor, sslSessionId, "ssl", "sessionId");
        setString(cursor, sslCipherSuite, "ssl", "cipherSuite");
        setString(cursor, sslPeerSubject, "ssl", "peerSubject");
        setTimestamp(cursor, sslPeerNotBefore, "ssl", "peerNotBefore");
        setTimestamp(cursor, sslPeerNotAfter, "ssl", "peerNotAfter");
        setString(cursor, sslSniServerName, "ssl", "sniServerName");
        setString(cursor, sslHandshakeFailureException, "ssl", "handshake-failure", "exception");
        setString(cursor, sslHandshakeFailureMessage, "ssl", "handshake-failure", "message");
        setString(cursor, sslHandshakeFailureType, "ssl", "handshake-failure", "type");
        return new String(Exceptions.uncheck(() -> SlimeUtils.toJsonBytes(slime)), StandardCharsets.UTF_8);
    }

    private void setString(Cursor cursor, String value, String... keys) {
        if(value != null) {
            subCursor(cursor, keys).setString(keys[keys.length - 1], value);
        }
    }

    private void setLong(Cursor cursor, Long value, String... keys) {
        if (value != null) {
            subCursor(cursor, keys).setLong(keys[keys.length - 1], value);
        }
    }

    private void setInteger(Cursor cursor, Integer value, String... keys) {
        if (value != null) {
            subCursor(cursor, keys).setLong(keys[keys.length - 1], value);
        }
    }

    private void setTimestamp(Cursor cursor, Instant timestamp, String... keys) {
        if (timestamp != null) {
            subCursor(cursor, keys).setString(keys[keys.length - 1], timestamp.toString());
        }
    }

    private void setDouble(Cursor cursor, Double value, String... keys) {
        if (value != null) {
            subCursor(cursor, keys).setDouble(keys[keys.length - 1], value);
        }
    }

    private static Cursor subCursor(Cursor cursor, String... keys) {
        Cursor subCursor = cursor;
        for (int i = 0; i < keys.length - 1; ++i) {
            Cursor field = subCursor.field(keys[i]);
            subCursor = field.type() != Type.NIX ? field : subCursor.setObject(keys[i]);
        }
        return subCursor;
    }

    public static Builder builder(UUID id, Instant timestamp) {
        return new Builder(id, timestamp);
    }

    public String id() { return id.toString(); }
    public Instant timestamp() { return timestamp; }
    public Optional<Double> durationSeconds() { return Optional.ofNullable(durationSeconds); }
    public Optional<String> peerAddress() { return Optional.ofNullable(peerAddress); }
    public Optional<Integer> peerPort() { return Optional.ofNullable(peerPort); }
    public Optional<String> localAddress() { return Optional.ofNullable(localAddress); }
    public Optional<Integer> localPort() { return Optional.ofNullable(localPort); }
    public Optional<String> remoteAddress() { return Optional.ofNullable(remoteAddress); }
    public Optional<Integer> remotePort() { return Optional.ofNullable(remotePort); }
    public Optional<Long> httpBytesReceived() { return Optional.ofNullable(httpBytesReceived); }
    public Optional<Long> httpBytesSent() { return Optional.ofNullable(httpBytesSent); }
    public Optional<Long> requests() { return Optional.ofNullable(requests); }
    public Optional<Long> responses() { return Optional.ofNullable(responses); }
    public Optional<String> sslSessionId() { return Optional.ofNullable(sslSessionId); }
    public Optional<String> sslProtocol() { return Optional.ofNullable(sslProtocol); }
    public Optional<String> sslCipherSuite() { return Optional.ofNullable(sslCipherSuite); }
    public Optional<String> sslPeerSubject() { return Optional.ofNullable(sslPeerSubject); }
    public Optional<Instant> sslPeerNotBefore() { return Optional.ofNullable(sslPeerNotBefore); }
    public Optional<Instant> sslPeerNotAfter() { return Optional.ofNullable(sslPeerNotAfter); }
    public Optional<String> sslSniServerName() { return Optional.ofNullable(sslSniServerName); }
    public Optional<String> sslHandshakeFailureException() { return Optional.ofNullable(sslHandshakeFailureException); }
    public Optional<String> sslHandshakeFailureMessage() { return Optional.ofNullable(sslHandshakeFailureMessage); }
    public Optional<String> sslHandshakeFailureType() { return Optional.ofNullable(sslHandshakeFailureType); }

    public static class Builder {
        private final UUID id;
        private final Instant timestamp;
        private Double durationSeconds;
        private String peerAddress;
        private Integer peerPort;
        private String localAddress;
        private Integer localPort;
        private String remoteAddress;
        private Integer remotePort;
        private Long httpBytesReceived;
        private Long httpBytesSent;
        private Long requests;
        private Long responses;
        private String sslSessionId;
        private String sslProtocol;
        private String sslCipherSuite;
        private String sslPeerSubject;
        private Instant sslPeerNotBefore;
        private Instant sslPeerNotAfter;
        private String sslSniServerName;
        private String sslHandshakeFailureException;
        private String sslHandshakeFailureMessage;
        private String sslHandshakeFailureType;


        Builder(UUID id, Instant timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }

        public Builder withDuration(double durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder withPeerAddress(String peerAddress) {
            this.peerAddress = peerAddress;
            return this;
        }
        public Builder withPeerPort(int peerPort) {
            this.peerPort = peerPort;
            return this;
        }
        public Builder withLocalAddress(String localAddress) {
            this.localAddress = localAddress;
            return this;
        }
        public Builder withLocalPort(int localPort) {
            this.localPort = localPort;
            return this;
        }
        public Builder withRemoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
            return this;
        }
        public Builder withRemotePort(int remotePort) {
            this.remotePort = remotePort;
            return this;
        }
        public Builder withHttpBytesReceived(long bytesReceived) {
            this.httpBytesReceived = bytesReceived;
            return this;
        }
        public Builder withHttpBytesSent(long bytesSent) {
            this.httpBytesSent = bytesSent;
            return this;
        }
        public Builder withRequests(long requests) {
            this.requests = requests;
            return this;
        }
        public Builder withResponses(long responses) {
            this.responses = responses;
            return this;
        }
        public Builder withSslSessionId(String sslSessionId) {
            this.sslSessionId = sslSessionId;
            return this;
        }
        public Builder withSslProtocol(String sslProtocol) {
            this.sslProtocol = sslProtocol;
            return this;
        }
        public Builder withSslCipherSuite(String sslCipherSuite) {
            this.sslCipherSuite = sslCipherSuite;
            return this;
        }
        public Builder withSslPeerSubject(String sslPeerSubject) {
            this.sslPeerSubject = sslPeerSubject;
            return this;
        }
        public Builder withSslPeerNotBefore(Instant sslPeerNotBefore) {
            this.sslPeerNotBefore = sslPeerNotBefore;
            return this;
        }
        public Builder withSslPeerNotAfter(Instant sslPeerNotAfter) {
            this.sslPeerNotAfter = sslPeerNotAfter;
            return this;
        }
        public Builder withSslSniServerName(String sslSniServerName) {
            this.sslSniServerName = sslSniServerName;
            return this;
        }
        public Builder withSslHandshakeFailureException(String exception) {
            this.sslHandshakeFailureException = exception;
            return this;
        }
        public Builder withSslHandshakeFailureMessage(String message) {
            this.sslHandshakeFailureMessage = message;
            return this;
        }
        public Builder withSslHandshakeFailureType(String type) {
            this.sslHandshakeFailureType = type;
            return this;
        }

        public ConnectionLogEntry build(){
            return new ConnectionLogEntry(this);
        }
    }
}
