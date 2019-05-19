package io.hfgbarrigas.reactive.dbcontext.context;

import io.hfgbarrigas.reactive.dbcontext.db.Connection;

public interface TransactionalContext {
    Connection getConnection();
}
