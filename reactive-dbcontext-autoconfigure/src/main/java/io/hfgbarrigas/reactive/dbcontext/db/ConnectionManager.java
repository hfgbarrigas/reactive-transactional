package io.hfgbarrigas.reactive.dbcontext.db;

import io.hfgbarrigas.reactive.dbcontext.context.TransactionalContext;
import reactor.core.publisher.Mono;

public interface ConnectionManager {
    Mono<? extends TransactionalContext> getConnection(boolean isTransactional);
    Mono<? extends TransactionalContext> getConnection();
}
