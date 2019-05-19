package io.hfgbarrigas.reactive.dbcontext.pg;

import io.hfgbarrigas.reactive.dbcontext.db.Connection;
import io.reactiverse.pgclient.PgConnection;
import io.reactiverse.pgclient.PgTransaction;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class PgConnectionAdapter implements Connection {

    private PgConnection connection;
    private PgTransaction transaction;

    private PgConnectionAdapter() {
    }

    public PgConnectionAdapter(PgConnection connection, PgTransaction transaction) {
        Objects.requireNonNull(connection);
        this.connection = connection;
        this.transaction = transaction;
    }

    @Override
    public Mono<Void> close() {
        return Mono.create(sink -> {
            this.connection.close();
            sink.success();
        });
    }

    @Override
    public Mono<Void> commit() {
        Objects.requireNonNull(transaction, "A transaction must exist to commit.");
        return Mono.create(sink -> this.transaction.commit(ar -> {
            if (ar.failed()) {
                sink.error(ar.cause());
            } else {
                sink.success();
            }
        }));
    }

    @Override
    public Mono<Void> rollback() {
        Objects.requireNonNull(transaction, "A transaction must exist to rollback.");
        return Mono.create(sink -> this.transaction.rollback(ar -> {
            if(ar.failed()) {
                sink.error(ar.cause());
            } else {
                sink.success();
            }
        }));
    }

    @Override
    public boolean isTransactional() {
        return this.transaction != null;
    }

    public PgConnection getConnection() {
        return connection;
    }

    public PgTransaction getTransaction() {
        return transaction;
    }
}
