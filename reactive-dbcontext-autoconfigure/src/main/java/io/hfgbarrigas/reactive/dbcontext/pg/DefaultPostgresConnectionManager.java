package io.hfgbarrigas.reactive.dbcontext.pg;

import io.hfgbarrigas.reactive.dbcontext.context.DefaultTransactionalContext;
import io.hfgbarrigas.reactive.dbcontext.context.TransactionalContext;
import io.hfgbarrigas.reactive.dbcontext.db.ConnectionManager;
import io.reactiverse.pgclient.PgConnection;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgTransaction;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

public class DefaultPostgresConnectionManager implements ConnectionManager {

    private PgPool pool;

    private DefaultPostgresConnectionManager() {
    }

    public DefaultPostgresConnectionManager(PgPool pool) {
        Objects.requireNonNull(pool);
        this.pool = pool;
    }

    /**
     * Asynchronously retrieves a connection from the database pool.
     * Furthermore, when a connection is obtained and isTransactional is true, a transaction is started.
     *
     * @return a {@code Mono<TransactionalContext>} encapsulating the connection and the transaction
     */
    @Override
    public Mono<? extends TransactionalContext> getConnection(boolean isTransactional) {
        return Mono.create(sink -> pool.getConnection(ac -> {
            if (ac.succeeded()) {
                PgConnection connection = ac.result();
                PgTransaction transaction = null;
                if(isTransactional)
                    transaction = connection.begin();

                sink.success(new DefaultTransactionalContext(new PgConnectionAdapter(connection, Optional.ofNullable(transaction).orElse(null))));
            } else {
                sink.error(ac.cause());
            }
        }));
    }

    /**
     * Asynchronously retrieves a connection from the database pool.
     *
     * @return a {@code Mono<TransactionalContext>} encapsulating the connection
     */
    @Override
    public Mono<? extends TransactionalContext> getConnection() {
        return getConnection(false);
    }
}
