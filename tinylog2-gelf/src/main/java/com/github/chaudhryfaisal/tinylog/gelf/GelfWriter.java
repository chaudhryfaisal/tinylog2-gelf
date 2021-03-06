package com.github.chaudhryfaisal.tinylog.gelf;

import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.tinylog.Level;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.Writer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A tinylog {@link org.tinylog.writers.Writer} writing log messages to a GELF-compatible server like
 * <a href="http://www.graylog2.org/">Graylog2</a>.
 */
public final class GelfWriter implements Writer {
    private static final boolean GLEF_DEBUG = Boolean.parseBoolean(System.getProperty("GLEF_DEBUG", "true"));
    private static final String FIELD_SEPARATOR = ":";
    private static final EnumSet<LogEntryValue> BASIC_LOG_ENTRY_VALUES = EnumSet.of(
            LogEntryValue.DATE,
            LogEntryValue.LEVEL,
            LogEntryValue.MESSAGE
    );

    private String server;
    private int port;
    private GelfTransports transport;
    private String hostname;
    private Set<LogEntryValue> requiredLogEntryValues;
    private Map<String, Object> staticFields;
    private int queueSize;
    private int connectTimeout;
    private int reconnectDelay;
    private int sendBufferSize;
    private boolean tcpNoDelay;

    private GelfTransport client;

    /**
     * Construct a new GelfWriter instance.
     *
     * @param server                 the hostname of the GELF-compatible server
     * @param port                   the port of the GELF-compatible server
     * @param transport              the transport protocol to use
     * @param hostname               the hostname of the application
     * @param requiredLogEntryValues additional information for log messages, see {@link LogEntryValue}
     * @param staticFields           additional static fields for the GELF messages
     * @param queueSize              the size of the internal queue the GELF client is using
     * @param connectTimeout         the connection timeout for TCP connections in milliseconds
     * @param reconnectDelay         the time to wait between reconnects in milliseconds
     * @param sendBufferSize         the size of the socket send buffer in bytes; a value of {@code -1}
     *                               deactivates the socket send buffer.
     * @param tcpNoDelay             {@code true} if Nagle's algorithm should used for TCP connections,
     *                               {@code false} otherwise
     */
    public GelfWriter(final String server,
                      final int port,
                      final GelfTransports transport,
                      final String hostname,
                      final Set<LogEntryValue> requiredLogEntryValues,
                      final Map<String, Object> staticFields,
                      final int queueSize,
                      final int connectTimeout,
                      final int reconnectDelay,
                      final int sendBufferSize,
                      final boolean tcpNoDelay) {
        this.server = server;
        this.port = port;
        this.transport = transport;
        this.hostname = buildHostName(hostname);
        this.requiredLogEntryValues = buildRequiredLogEntryValues(requiredLogEntryValues);
        this.staticFields = staticFields;
        this.queueSize = queueSize;
        this.connectTimeout = connectTimeout;
        this.reconnectDelay = reconnectDelay;
        this.sendBufferSize = sendBufferSize;
        this.tcpNoDelay = tcpNoDelay;
        init();
    }

    private void init() {
        final InetSocketAddress remoteAddress = new InetSocketAddress(server, port);
        final GelfConfiguration gelfConfiguration = new GelfConfiguration(remoteAddress)
                .transport(transport)
                .queueSize(queueSize)
                .connectTimeout(connectTimeout)
                .reconnectDelay(reconnectDelay)
                .sendBufferSize(sendBufferSize)
                .tcpNoDelay(tcpNoDelay);

        client = GelfTransports.create(gelfConfiguration);
    }

    private static EnumSet<LogEntryValue> buildLogEntryValuesFromString(String... logEntryValues) {
        final EnumSet<LogEntryValue> result = EnumSet.noneOf(LogEntryValue.class);

        for (String logEntryValue : logEntryValues) {
            result.add(LogEntryValue.valueOf(logEntryValue));
        }

        return result;
    }

    private static EnumSet<LogEntryValue> buildRequiredLogEntryValues(Set<LogEntryValue> additionalValues) {
        final EnumSet<LogEntryValue> result = EnumSet.copyOf(additionalValues);
        result.addAll(BASIC_LOG_ENTRY_VALUES);
        return result;
    }

    private String buildHostName(final String hostname) {
        if (null == hostname || hostname.isEmpty()) {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                return "localhost";
            }
        }

        return hostname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<LogEntryValue> getRequiredLogEntryValues() {
        return requiredLogEntryValues;
    }

    /**
     * {@inheritDoc}
     */

    public GelfWriter(Map<String, String> p) {
        this(p.getOrDefault("server", "localhost"),
                Integer.parseInt(p.getOrDefault("port", "12201")),
                GelfTransports.valueOf(p.getOrDefault("transport", "UDP")),
                p.getOrDefault("hostname", null),
                buildLogEntryValuesFromString(p.getOrDefault("additionalLogEntryValues", "EXCEPTION")),
                Arrays.stream(p.getOrDefault("staticFields", "").split(",")).map(s -> s.split(FIELD_SEPARATOR)).collect(Collectors.toMap(s -> s[0], s -> s[1])),
                Integer.parseInt(p.getOrDefault("queueSize", "512")),
                Integer.parseInt(p.getOrDefault("connectTimeout", "1000")),
                Integer.parseInt(p.getOrDefault("recoveryTimeout", "500")),
                Integer.parseInt(p.getOrDefault("sendBufferSize", "-1")),
                Boolean.parseBoolean(p.getOrDefault("tcpNoDelay", "false"))
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final LogEntry logEntry) throws Exception {
        if (logEntry.getClassName() == null) {
            if (GLEF_DEBUG) {
                System.err.printf("className is null %s\n", logEntry.getMessage());
            }
        } else {
            write(client, logEntry);
        }
    }

    void write(final GelfTransport gelfClient, final LogEntry logEntry) throws Exception {
        final String message = logEntry.getMessage();
        final GelfMessageBuilder messageBuilder = new GelfMessageBuilder(message, hostname)
                .timestamp(logEntry.getTimestamp().toInstant().toEpochMilli())
                .level(toGelfMessageLevel(logEntry.getLevel()))
                .additionalFields(staticFields);

        final Thread thread = logEntry.getThread();
        if (null != thread) {
            messageBuilder.additionalField("threadName", thread.getName());
            messageBuilder.additionalField("threadGroup", thread.getThreadGroup().getName());
            messageBuilder.additionalField("threadPriority", thread.getPriority());
        }

        final String className = logEntry.getClassName();
        if (null != className) {
            messageBuilder.additionalField("sourceClassName", className);
        }

        final String methodName = logEntry.getMethodName();
        if (null != methodName && !"<unknown>".equals(methodName)) {
            messageBuilder.additionalField("sourceMethodName", methodName);
        }

        final String fileName = logEntry.getFileName();
        if (null != fileName) {
            messageBuilder.additionalField("sourceFileName", fileName);
        }

        final int lineNumber = logEntry.getLineNumber();
        if (lineNumber != -1) {
            messageBuilder.additionalField("sourceLineNumber", lineNumber);
        }

        @SuppressWarnings("all") final Throwable throwable = logEntry.getException();
        if (null != throwable) {
            final StringBuilder stackTraceBuilder = new StringBuilder();
            for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
                new Formatter(stackTraceBuilder)
                        .format("%s.%s(%s:%d)%n",
                                stackTraceElement.getClassName(), stackTraceElement.getMethodName(),
                                stackTraceElement.getFileName(), stackTraceElement.getLineNumber());
            }

            messageBuilder.additionalField("exceptionClass", throwable.getClass().getCanonicalName());
            messageBuilder.additionalField("exceptionMessage", throwable.getMessage());
            messageBuilder.additionalField("exceptionStackTrace", stackTraceBuilder.toString());
            messageBuilder.fullMessage(message + "\n\n" + stackTraceBuilder.toString());
        }

        gelfClient.send(messageBuilder.build());
    }

    private GelfMessageLevel toGelfMessageLevel(final Level level) {
        switch (level) {
            case TRACE:
            case DEBUG:
                return GelfMessageLevel.DEBUG;
            case INFO:
                return GelfMessageLevel.INFO;
            case WARN:
                return GelfMessageLevel.WARNING;
            case ERROR:
                return GelfMessageLevel.ERROR;
            default:
                throw new IllegalArgumentException("Invalid log level " + level);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws Exception {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
//        VMShutdownHook.unregister(this);
        if (client != null) {
            client.stop();
        }
    }
}
