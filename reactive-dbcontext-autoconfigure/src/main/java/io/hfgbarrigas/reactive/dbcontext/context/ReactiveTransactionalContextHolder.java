package io.hfgbarrigas.reactive.dbcontext.context;

import io.hfgbarrigas.reactive.dbcontext.exceptions.DatabaseContextNotPresent;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Allows getting and setting the {@link TransactionalContext} from/into a {@link Context}.
 *
 * @author Hugo Barrigas
 */
public class ReactiveTransactionalContextHolder {

    private static final Class<?> DATABASE_CONNECTION_CONTEXT_KEY = TransactionalContext.class;

    /**
     * Gets the {@code Mono<TransactionalContext>} from Reactor {@link Context}
     * @return the {@code Mono<TransactionalContext>}
     */
    public static Mono<TransactionalContext> getContext() {
        return Mono.subscriberContext()
                .filter( c -> c.hasKey(DATABASE_CONNECTION_CONTEXT_KEY))
                .flatMap( c-> c.<Mono<TransactionalContext>>get(DATABASE_CONNECTION_CONTEXT_KEY))
                .switchIfEmpty(Mono.error(new DatabaseContextNotPresent("No database context was present.")));
    }

    /**
     * Clears the {@code Mono<TransactionalContext>} from Reactor {@link Context}
     * @return a new {@link Context} resulting from the deletion.
     */
    public static Context clearContext(Context ctx) {
        return ctx.delete(DATABASE_CONNECTION_CONTEXT_KEY);
    }

    /**
     * Creates a Reactor {@link Context} that contains the {@code Mono<TransactionalContext>}
     * that can be merged into another {@link Context}
     * @param databaseConnection the {@code Mono<TransactionalContext>} to set in the returned
     * Reactor {@link Context}
     * @return a Reactor {@link Context} that contains the {@code Mono<TransactionalContext>}
     */
    public static Context withDatabaseConnectionContext(Mono<? extends TransactionalContext> databaseConnection) {
        return Context.of(DATABASE_CONNECTION_CONTEXT_KEY, databaseConnection);
    }

    /**
     * Provide the existing {@link TransactionalContext} present in the context, otherwise just return an empty mono.
     * @param context - {@link Context}
     * @return {@link TransactionalContext}
     */
    public static Mono<TransactionalContext> getDatabaseConnectionContext(Context context) {
        return context.hasKey(DATABASE_CONNECTION_CONTEXT_KEY) ? context.<Mono<TransactionalContext>>get(DATABASE_CONNECTION_CONTEXT_KEY) : Mono.empty();
    }

    /**
     * @param context {@link Context}
     * @return whether the provided context has a database connection context
     */
    public static boolean hasDatabaseConnectionContext(Context context) {
        return context.hasKey(DATABASE_CONNECTION_CONTEXT_KEY);
    }
}
