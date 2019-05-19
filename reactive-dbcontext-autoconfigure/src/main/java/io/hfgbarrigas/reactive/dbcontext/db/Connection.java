package io.hfgbarrigas.reactive.dbcontext.db;

import reactor.core.publisher.Mono;

public interface Connection {
    Mono<Void> close();
    Mono<Void> commit();
    Mono<Void> rollback();
    boolean isTransactional();
}
