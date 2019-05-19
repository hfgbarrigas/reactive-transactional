package io.hfgbarrigas.reactive.dbcontext.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("reactive.dbcontext.dbcontext")
public class PostgresProperties {
    private String host;
    private String user;
    private String password;
    private String database;
    private int idleTimeout = 0;
    private int connectTimeout = 0;
    private String metricsName;
    private int port = 5432;
    private int poolMaxSize = 5;
    private boolean tcpKeepAlive = true;
    private boolean tcpQuickAck = false;
    private boolean tcpFastOpen = false;
    private boolean tcpNoDelay = true;
    private boolean cachePreparedStatements = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getMetricsName() {
        return metricsName;
    }

    public void setMetricsName(String metricsName) {
        this.metricsName = metricsName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    public void setPoolMaxSize(int poolMaxSize) {
        this.poolMaxSize = poolMaxSize;
    }

    public boolean isTcpKeepAlive() {
        return tcpKeepAlive;
    }

    public void setTcpKeepAlive(boolean tcpKeepAlive) {
        this.tcpKeepAlive = tcpKeepAlive;
    }

    public boolean isTcpQuickAck() {
        return tcpQuickAck;
    }

    public void setTcpQuickAck(boolean tcpQuickAck) {
        this.tcpQuickAck = tcpQuickAck;
    }

    public boolean isTcpFastOpen() {
        return tcpFastOpen;
    }

    public void setTcpFastOpen(boolean tcpFastOpen) {
        this.tcpFastOpen = tcpFastOpen;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public boolean isCachePreparedStatements() {
        return cachePreparedStatements;
    }

    public void setCachePreparedStatements(boolean cachePreparedStatements) {
        this.cachePreparedStatements = cachePreparedStatements;
    }
}
