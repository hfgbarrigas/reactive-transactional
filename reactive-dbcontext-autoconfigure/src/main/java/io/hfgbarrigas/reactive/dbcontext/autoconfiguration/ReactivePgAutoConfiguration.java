package io.hfgbarrigas.reactive.dbcontext.autoconfiguration;

import io.hfgbarrigas.reactive.dbcontext.aspect.TransactionalContextAspect;
import io.hfgbarrigas.reactive.dbcontext.db.ConnectionManager;
import io.hfgbarrigas.reactive.dbcontext.pg.DefaultPostgresConnectionManager;
import io.hfgbarrigas.reactive.dbcontext.properties.PostgresProperties;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@EnableConfigurationProperties(PostgresProperties.class)
public class ReactivePgAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PgPoolOptions.class)
    public PgPoolOptions pgPoolOptions(PostgresProperties properties) {
        return new PgPoolOptions()
                .setMaxSize(properties.getPoolMaxSize())
                .setConnectTimeout(properties.getConnectTimeout())
                .setDatabase(properties.getDatabase())
                .setHost(properties.getHost())
                .setIdleTimeout(properties.getIdleTimeout())
                .setMetricsName(properties.getMetricsName())
                .setPassword(properties.getPassword())
                .setPort(properties.getPort())
                .setTcpKeepAlive(properties.isTcpKeepAlive())
                .setTcpQuickAck(properties.isTcpQuickAck())
                .setTcpFastOpen(properties.isTcpFastOpen())
                .setTcpNoDelay(properties.isTcpNoDelay())
                .setCachePreparedStatements(properties.isCachePreparedStatements())
                .setUser(properties.getUser())
                .setMetricsName("pgclient");
    }

    @Bean
    @ConditionalOnBean(PgPoolOptions.class)
    @ConditionalOnMissingBean(PgPool.class)
    public PgPool pgPool(PgPoolOptions pgPoolOptions) {
        return PgClient.pool(pgPoolOptions);
    }

    @Bean
    public TransactionalContextAspect dbConnectionAspect(ConnectionManager connectionManager) {
        return new TransactionalContextAspect(connectionManager);
    }

    @Bean("pgConnectionManager")
    @ConditionalOnBean(PgPool.class)
    @ConditionalOnMissingBean(ConnectionManager.class)
    public ConnectionManager pgConnectionManager(PgPool pool) {
        return new DefaultPostgresConnectionManager(pool);
    }
}
