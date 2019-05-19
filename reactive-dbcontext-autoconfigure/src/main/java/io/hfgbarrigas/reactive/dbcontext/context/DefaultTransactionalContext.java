package io.hfgbarrigas.reactive.dbcontext.context;

import io.hfgbarrigas.reactive.dbcontext.db.Connection;

import java.util.Objects;

public class DefaultTransactionalContext implements TransactionalContext {

    private Connection connection;

    private DefaultTransactionalContext() {
    }

    public DefaultTransactionalContext(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Connection getConnection() {
        return this.connection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultTransactionalContext that = (DefaultTransactionalContext) o;
        return connection.equals(that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connection);
    }

    @Override
    public String toString() {
        return "DefaultTransactionalContext{" +
                "connection=" + connection +
                '}';
    }
}
