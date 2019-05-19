package io.hfgbarrigas.reactive.dbcontext.aspect;

import io.hfgbarrigas.reactive.dbcontext.context.ReactiveTransactionalContextHolder;
import io.hfgbarrigas.reactive.dbcontext.db.ConnectionManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Implements the {@link TransactionalContext} aspect.
 * Allows database connection share through {@link Context}. All underlying database accesses will share the connection. Also the transaction if the connection is marked as write.
 */
@Aspect
public class TransactionalContextAspect {

    static final String DATABASE_CONNECTION_CONTEXT_CREATOR = "DATABASE_CONNECTION_CONTEXT_CREATOR";
    private ConnectionManager connectionManager;

    public TransactionalContextAspect(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }


    /**
     * Defines a pointcut responsible for checking whether <em>a method is annotated</em> with {@link TransactionalContext}.
     *
     * @param transactionalContext the annotation from which the connection type can be retrieved
     * @see TransactionalContext
     */
    @Pointcut("@annotation(transactionalContext)")
    public void hasAnnotationInMethod(TransactionalContext transactionalContext) {
    }


    /**
     * Defines a pointcut responsible for checking whether <em>a class is annotated</em> with {@link TransactionalContext},
     * and, therefore, <em>all class methods</em> are eligible for database connection management and share through {@link Context}.
     *
     * @param transactionalContext the annotation from which the connection type can be retrieved
     * @see TransactionalContext
     */
    @Pointcut("@within(transactionalContext)")
    public void hasAnnotationInClass(TransactionalContext transactionalContext) {
    }


    /**
     * Defines a pointcut responsible for checking whether the (possibly annotated) method has the following characteristics:
     * <ul>
     * <li><em>Return type:</em> {@link Mono}</li>
     * </ul>
     *
     * @see Mono
     */
    @Pointcut("execution(reactor.core.publisher.Mono *(..))")
    public void isMonoReturn() {
    }


    /**
     * Defines a pointcut responsible for checking whether the (possibly annotated) method has the following characteristics:
     * <ul>
     * <li><em>Return type:</em> {@link Flux}</li>
     * </ul>
     *
     * @see Flux
     */
    @Pointcut("execution(reactor.core.publisher.Flux *(..))")
    public void isFluxReturn() {
    }


    /**
     * Checks whether the eligible method has the expected return types:
     * <ul>
     * <li>{@link Mono}</li>
     * <li>{@link Flux}</li>
     * </ul>
     *
     * @see #isMonoReturn()
     * @see #isFluxReturn()
     * @see Mono
     * @see Flux
     */
    @Pointcut("isMonoReturn() || isFluxReturn()")
    public void validReturn() {
    }


    /**
     * Represents the advice of the {@link TransactionalContext} aspect.
     * Note that this is an <code>Around</code> advice.
     * <p>
     * Starts of checking whether an existing database connection is present in the context.
     * <p>
     * If it is a non-null value, the aspect assumes that a connection is already open, so the aspect will reuse it.
     * <p>
     * Otherwise, the aspect will request a new connection from the database pool and start a new transaction. The selected pool is based on the value of {@link TransactionalContext#value()} ()} value.
     * The connection is then injected in the first parameter of the invoked method.
     * Since the expected result type is {@link Mono} or {@link Flux}, as stated in {@link #isMonoReturn()} and {@link #isFluxReturn()}, callback hooks are used to commit the results or rollback, depending on the completeness status.
     * </p>
     *
     * @param proceedingJoinPoint          the proceeding join point, containing the arguments which were bound to the method's invocation
     * @param transactionalContext the aspect annotation. The pool type is extracted from the annotation method {@link TransactionalContext#value()} ()}
     * @return the result of the execution of the method. As aforementioned in {@link #isMonoReturn()} and {@link #isFluxReturn()}, the result is of type {@link Mono} or {@link Flux}
     * @throws Throwable any throwable resulted by the execution of the advised method
     */
    @Around(value = "hasAnnotationInClass(transactionalContext) && validReturn()", argNames = "proceedingJoinPoint,transactionalContext")
    public Object aroundAtClassLevel(ProceedingJoinPoint proceedingJoinPoint, TransactionalContext transactionalContext) throws Throwable {
        return around(proceedingJoinPoint, transactionalContext);
    }

    @Around(value = "hasAnnotationInMethod(transactionalContext) && validReturn()", argNames = "proceedingJoinPoint,transactionalContext")
    public Object aroundAtMethodLevel(ProceedingJoinPoint proceedingJoinPoint, TransactionalContext transactionalContext) throws Throwable {
        return around(proceedingJoinPoint, transactionalContext);
    }

    Object around(ProceedingJoinPoint proceedingJoinPoint, TransactionalContext transactionalContext) throws Throwable {

        String methodName = proceedingJoinPoint.getSignature().getDeclaringTypeName() + "." + proceedingJoinPoint.getSignature().getName();
        Object result = proceedingJoinPoint.proceed(proceedingJoinPoint.getArgs());

        if (result instanceof Mono<?>) {
            return Mono.usingWhen(Mono.subscriberContext(),
                    ctx -> (Mono<?>) result,
                    ctx -> successfulClosure(ctx, methodName),
                    ctx -> errorClosure(ctx, methodName),
                    ctx -> errorClosure(ctx, methodName))
                    .subscriberContext(enrichContext(transactionalContext.value(), methodName));
        } else {
            return Flux.usingWhen(Mono.subscriberContext(),
                    ctx -> (Flux<?>) result,
                    ctx -> successfulClosure(ctx, methodName),
                    ctx -> errorClosure(ctx, methodName),
                    ctx -> errorClosure(ctx, methodName))
                    .subscriberContext(enrichContext(transactionalContext.value(), methodName));
        }
    }

    /**
     * Enrich current context with a new database connection if there're none present
     *
     * @param type - Type of database connection needed
     * @return - Functional context decorator
     */
    private Function<Context, Context> enrichContext(TransactionalContextType type, String methodName) {
        requireNonNull(type);
        requireNonNull(methodName);

        return ctx -> {
            if (TransactionalContextType.WRITE.equals(type)) {
                return ReactiveTransactionalContextHolder.hasDatabaseConnectionContext(ctx) ? ctx :
                        ctx.putAll(ReactiveTransactionalContextHolder.withDatabaseConnectionContext(connectionManager.getConnection(true).cache()))
                                .put(DATABASE_CONNECTION_CONTEXT_CREATOR, methodName);
            } else {
                return ReactiveTransactionalContextHolder.hasDatabaseConnectionContext(ctx) ? ctx :
                        ctx.putAll(ReactiveTransactionalContextHolder.withDatabaseConnectionContext(connectionManager.getConnection().cache()))
                                .put(DATABASE_CONNECTION_CONTEXT_CREATOR, methodName);
            }
        };
    }

    /**
     * Conditionally closes any the database connection and commits/rollsback any connection and transaction present in the context.
     * The action will only be performed when the signal received comes from the mono that created the database context.
     *
     * @param methodName the method from which signals are being received
     */
    private Mono<Void> errorClosure(Context ctx, String methodName) {
        requireNonNull(ctx);
        requireNonNull(methodName);

        String creator = ctx.getOrDefault(DATABASE_CONNECTION_CONTEXT_CREATOR, "");

        return ReactiveTransactionalContextHolder.getDatabaseConnectionContext(ctx)
                .flatMap(databaseConnectionContext -> {
                    if (methodName.equals(creator)) {
                        if (databaseConnectionContext.getConnection().isTransactional()) {
                            return databaseConnectionContext.getConnection()
                                    .rollback()
                                    .onErrorResume(err -> databaseConnectionContext.getConnection().close().then(Mono.error(err)))
                                    .thenEmpty(databaseConnectionContext.getConnection().close());
                        } else {
                            return databaseConnectionContext.getConnection().close();
                        }
                    } else {
                        return Mono.empty();
                    }
                });
    }

    /**
     * Returns a connection to the pool and if there's a transaction present in the context, it will be commited.
     *
     * @param ctx the database connection and respective transaction on which a commit is going to be executed
     */
    private Mono<Void> successfulClosure(Context ctx, String methodName) {
        requireNonNull(methodName);

        return ReactiveTransactionalContextHolder.getDatabaseConnectionContext(ctx)
                .flatMap(databaseConnectionContext -> {
                    String dbContextCreator = ctx.getOrDefault(DATABASE_CONNECTION_CONTEXT_CREATOR, "");
                    if (methodName.equals(dbContextCreator)) {
                        if (databaseConnectionContext.getConnection().isTransactional()) {
                            return databaseConnectionContext.getConnection().commit()
                                    .onErrorResume(err -> databaseConnectionContext.getConnection().rollback()
                                            .onErrorResume(err1 -> databaseConnectionContext.getConnection().close().then(Mono.error(err1)))
                                            .then(databaseConnectionContext.getConnection().close())
                                            .then(Mono.error(err)))
                                    .thenEmpty(databaseConnectionContext.getConnection().close());
                        } else {
                            return databaseConnectionContext.getConnection().close();
                        }
                    } else {
                        return Mono.empty();
                    }
                });
    }
}
