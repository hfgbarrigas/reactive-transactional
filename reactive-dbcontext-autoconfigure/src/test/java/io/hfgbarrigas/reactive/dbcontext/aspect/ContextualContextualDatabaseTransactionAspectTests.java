package io.hfgbarrigas.reactive.dbcontext.aspect;

import io.hfgbarrigas.reactive.dbcontext.context.DefaultTransactionalContext;
import io.hfgbarrigas.reactive.dbcontext.context.TransactionalContext;
import io.hfgbarrigas.reactive.dbcontext.pg.DefaultPostgresConnectionManager;
import io.hfgbarrigas.reactive.dbcontext.pg.PgConnectionAdapter;
import io.reactiverse.pgclient.PgConnection;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgTransaction;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static io.hfgbarrigas.reactive.dbcontext.aspect.TransactionalContextAspect.DATABASE_CONNECTION_CONTEXT_CREATOR;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ContextualContextualDatabaseTransactionAspectTests {

    private PgPool pool = mock(PgPool.class);
    private PgTransaction transaction = mock(PgTransaction.class);
    private PgConnection connection = mock(PgConnection.class);
    private ProceedingJoinPoint proceedingJoinPoint = mock(ProceedingJoinPoint.class);
    private Signature signature = mock(Signature.class);
    private io.hfgbarrigas.reactive.dbcontext.aspect.TransactionalContext dbConnection = mock(io.hfgbarrigas.reactive.dbcontext.aspect.TransactionalContext.class);

    private TransactionalContextAspect transactionalContextAspect = new TransactionalContextAspect(new DefaultPostgresConnectionManager(pool));

    @Test
    public void shouldCreateDatabaseConnectionContextWithoutTransaction() throws Throwable {
        //prepare
        Mono<String> mono = Mono.just("data");

        when(dbConnection.value()).thenReturn(TransactionalContextType.READ);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        //act
        Mono<String> res = (Mono<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectNext("data")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertFalse(m.getConnection().isTransactional());
                })
                .then()
                .expectComplete()
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).close();
    }

    @Test
    public void shouldNotCreateDatabaseConnectionContextAndNotCloseItDueToMethodNameMismatch() throws Throwable {
        //prepare
        Mono<String> mono = Mono.just("data");

        when(dbConnection.value()).thenReturn(TransactionalContextType.READ);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);

        //act
        Mono<String> res = (Mono<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        res = res
                .subscriberContext(ctx -> ctx
                        .put(TransactionalContext.class, Mono.just(new DefaultTransactionalContext(new PgConnectionAdapter(connection, null))))
                );

        //assert
        StepVerifier.create(res)
                .expectNext("data")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(1)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertFalse(m.getConnection().isTransactional());
                })
                .then()
                .expectComplete()
                .verify();

        verify(pool, times(0)).getConnection(any());
        verify(connection, times(0)).close();
    }

    @Test
    public void shouldCreateDatabaseConnectionContextWithTransaction() throws Throwable {
        //prepare
        Mono<String> mono = Mono.just("data");

        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);
        when(connection.begin()).thenReturn(transaction);

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, null));
            return null;
        }).when(transaction).commit(any());

        //act
        Mono<String> res = (Mono<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectNext("data")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectComplete()
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).begin();
        verify(transaction, times(1)).commit(any());
        verify(connection, times(1)).close();
    }

    @Test
    public void shouldNotCreateDatabaseConnectionWithTransactionAndShouldNotCloseConnectionDueToMethodNameMismatch() throws Throwable {
        //prepare
        Mono<String> mono = Mono.just("data");

        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);

        //act
        Mono<String> res = (Mono<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        res = res
                .subscriberContext(ctx -> ctx.put(TransactionalContext.class, Mono.just(new DefaultTransactionalContext(new PgConnectionAdapter(connection, transaction))))
                        .put(DATABASE_CONNECTION_CONTEXT_CREATOR, "creator"));

        //assert
        StepVerifier.create(res)
                .expectNext("data")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectComplete()
                .verify();

        verify(pool, times(0)).getConnection(any());
        verify(connection, times(0)).begin();
        verify(transaction, times(0)).commit(any());
        verify(connection, times(0)).close();
    }

    @Test
    public void shouldCreateDatabaseConnectionContextWithoutTransactionWithFlux() throws Throwable {
        //prepare
        Flux<String> mono = Flux.just("data", "batata");

        when(dbConnection.value()).thenReturn(TransactionalContextType.READ);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);;

        //assert
        StepVerifier.create(res)
                .expectNext("data", "batata")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertFalse(m.getConnection().isTransactional());
                })
                .then()
                .expectComplete()
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).close();
    }

    @Test
    public void shouldNotCreateDatabaseConnectionContextAndNotCloseItDueToMethodNameMismatchWithFlux() throws Throwable {
        //prepare
        Flux<String> mono = Flux.just("data", "batata");

        when(dbConnection.value()).thenReturn(TransactionalContextType.READ);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        res = res
                .subscriberContext(ctx -> ctx
                        .put(TransactionalContext.class, Mono.just(new DefaultTransactionalContext(new PgConnectionAdapter(connection, transaction))))
                        .put(DATABASE_CONNECTION_CONTEXT_CREATOR, "creator")
                );

        //assert
        StepVerifier.create(res)
                .expectNext("data", "batata")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectComplete()
                .verify();

        verify(pool, times(0)).getConnection(any());
        verify(connection, times(0)).close();
    }

    @Test
    public void shouldCreateDatabaseConnectionContextWithTransactionWithFlux() throws Throwable {
        //prepare
        Flux<String> mono = Flux.just("data", "batata");

        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);
        when(connection.begin()).thenReturn(transaction);

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, null));
            return null;
        }).when(transaction).commit(any());

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectNext("data", "batata")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectComplete()
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).close();
    }

    @Test
    public void shouldNotCreateDatabaseConnectionWithTransactionAndShouldNotCloseConnectionOrCommitTxDueToMethodNameMismatchWithFlux() throws Throwable {
        //prepare
        Flux<String> mono = Flux.just("data");

        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        res = res
                .subscriberContext(ctx -> ctx.put(TransactionalContext.class, Mono.just(new DefaultTransactionalContext(new PgConnectionAdapter(connection, transaction))))
                        .put(DATABASE_CONNECTION_CONTEXT_CREATOR, "creator"));

        //assert
        StepVerifier.create(res)
                .expectNext("data")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectComplete()
                .verify();

        verify(pool, times(0)).getConnection(any());
        verify(connection, times(0)).begin();
        verify(transaction, times(0)).commit(any());
        verify(connection, times(0)).close();
    }

    @Test
    public void shouldThrowErrorOnTransactionalConnectionFetchError() throws Throwable {
        //prepare
        Flux<String> mono = Flux.just("data", "batata");

        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);
        when(connection.begin()).thenReturn(transaction);

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, new RuntimeException("potato")));
            return null;
        }).when(pool).getConnection(any());

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectNext("data", "batata")
                .verifyErrorMessage("Async resource cleanup failed after onComplete");

        verify(pool, times(1)).getConnection(any());
    }

    @Test
    public void shouldThrowErrorOnConnectionFetchError() throws Throwable {
        //prepare
        Flux<String> mono = Flux.just("data");

        when(dbConnection.value()).thenReturn(TransactionalContextType.READ);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);
        when(connection.begin()).thenReturn(transaction);

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, new RuntimeException("potato")));
            return null;
        }).when(pool).getConnection(any());

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectNext("data")
                .verifyErrorMessage("Async resource cleanup failed after onComplete"); //using this message to give a hint of the error I'm expecting when I coded this

        verify(pool, times(1)).getConnection(any());
    }

    @Test
    public void shouldCloseAndRollbackOnMethodExecutionError() throws Throwable {
        //prepare
        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(connection.begin()).thenReturn(transaction);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(Flux.error(new RuntimeException("potato")));

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, null));
            return null;
        }).when(transaction).rollback(any());

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectErrorMessage("potato")
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).begin();
        verify(connection, times(1)).close();
        verify(transaction, times(1)).rollback(any());
        verify(transaction, times(0)).commit(any());
    }

    @Test
    public void shouldThrowOnRollbackErrorOnMethodExecutionError() throws Throwable {
        //prepare
        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(connection.begin()).thenReturn(transaction);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(Flux.error(new RuntimeException("potato")));

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, new RuntimeException("rollback error")));
            return null;
        }).when(transaction).rollback(any());

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectErrorMessage("Async resource cleanup failed after onError")
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).begin();
        verify(connection, times(1)).close();
        verify(transaction, times(1)).rollback(any());
        verify(transaction, times(0)).commit(any());
    }

    @Test
    public void shouldCloseConnectionOnExecutionError() throws Throwable {
        //prepare
        when(dbConnection.value()).thenReturn(TransactionalContextType.READ);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(Flux.error(new RuntimeException("potato")));

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectErrorMessage("potato")
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).close();
    }

    @Test
    public void shouldNotCloseOrRollbackOnMethodExecutionErrorWhenMethodNameDoesNotMatchContextCreator() throws Throwable {
        //prepare
        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(connection.begin()).thenReturn(transaction);
        when(proceedingJoinPoint.proceed(any())).thenReturn(Mono.error(new RuntimeException("potato")));

        //act
        Mono<String> res = (Mono<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        res = res
                .subscriberContext(ctx -> ctx
                        .put(TransactionalContext.class, Mono.just(new DefaultTransactionalContext(new PgConnectionAdapter(connection, transaction))))
                        .put(DATABASE_CONNECTION_CONTEXT_CREATOR, "creator")
                );

        //assert
        StepVerifier.create(res)
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectErrorMessage("potato")
                .verify();

        verify(pool, times(0)).getConnection(any());
        verify(connection, times(0)).close();
        verify(transaction, times(0)).rollback();
    }

    @Test
    public void shouldCloseAndRollbackOnCancel() throws Throwable {
        //prepare
        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(connection.begin()).thenReturn(transaction);
        when(proceedingJoinPoint.proceed(any())).thenReturn(Mono.just("data"));

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, null));
            return null;
        }).when(transaction).rollback(any());

        //act
        Mono<String> res = (Mono<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectNext("data")
                .thenCancel()
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).begin();
        verify(connection, times(1)).close();
        verify(transaction, times(1)).rollback(any());
    }

    @Test
    public void shouldCloseAndRollbackOnCancelFlux() throws Throwable {
        //prepare
        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(connection.begin()).thenReturn(transaction);
        when(proceedingJoinPoint.proceed(any())).thenReturn(Flux.just("data", "batata"));

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, null));
            return null;
        }).when(transaction).rollback(any());

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectNext("data")
                .thenCancel()
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).begin();
        verify(connection, times(1)).close();
        verify(transaction, times(1)).rollback(any());
    }

    @Test
    public void shouldThrowAndStillCloseConnectionOnRollbackErrorWhenCancellingFlux() throws Throwable {
        //prepare
        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(connection.begin()).thenReturn(transaction);
        when(proceedingJoinPoint.proceed(any())).thenReturn(Flux.just("data", "batata"));

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, new RuntimeException("potato")));
            return null;
        }).when(transaction).rollback(any());

        //act
        Flux<String> res = (Flux<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectNext("data")
                .thenCancel()
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).begin();
        verify(connection, times(1)).close();
        verify(transaction, times(1)).rollback(any());
    }

    @Test
    public void shouldThrowOnTxCommitErrorAndProceedWithRollback() throws Throwable {
        //prepare
        Mono<String> mono = Mono.just("data");

        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);
        when(connection.begin()).thenReturn(transaction);

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, new RuntimeException("potato")));
            return null;
        }).when(transaction).commit(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, null));
            return null;
        }).when(transaction).rollback(any());

        //act
        Mono<String> res = (Mono<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectNext("data")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectErrorMessage("Async resource cleanup failed after onComplete")
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).begin();
        verify(transaction, times(1)).commit(any());
        verify(transaction, times(1)).rollback(any());
        verify(connection, times(1)).close();
    }

    @Test
    public void shouldThrowAndStillCloseConnectionOnCommitAndRollbackError() throws Throwable {
        //prepare
        Mono<String> mono = Mono.just("data");

        when(dbConnection.value()).thenReturn(TransactionalContextType.WRITE);
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("potato");
        when(signature.getDeclaringTypeName()).thenReturn("potato");
        when(proceedingJoinPoint.proceed(any())).thenReturn(mono);
        when(connection.begin()).thenReturn(transaction);

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<PgConnection>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(connection, null));
            return null;
        }).when(pool).getConnection(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, new RuntimeException("potato")));
            return null;
        }).when(transaction).commit(any());

        doAnswer((Answer<Void>) invocation -> {
            Handler<AsyncResult<Void>> handler = invocation.getArgument(0);
            handler.handle(asyncHandler(null, new RuntimeException("potato")));
            return null;
        }).when(transaction).rollback(any());

        //act
        Mono<String> res = (Mono<String>) transactionalContextAspect.around(proceedingJoinPoint, dbConnection);

        //assert
        StepVerifier.create(res)
                .expectNext("data")
                .expectAccessibleContext()
                .hasKey(TransactionalContext.class)
                .hasSize(2)
                .assertThat(ctx -> {
                    TransactionalContext m = ctx.<Mono<TransactionalContext>>get(TransactionalContext.class).block();
                    assertNotNull(m.getConnection());
                    assertTrue(m.getConnection().isTransactional());
                })
                .then()
                .expectErrorMessage("Async resource cleanup failed after onComplete")
                .verify();

        verify(pool, times(1)).getConnection(any());
        verify(connection, times(1)).begin();
        verify(transaction, times(1)).commit(any());
        verify(transaction, times(1)).rollback(any());
        verify(connection, times(1)).close();
    }

    private <T> AsyncResult<T> asyncHandler(T result, Throwable cause) {
        return new AsyncResult<T>() {
            @Override
            public T result() {
                return result;
            }

            @Override
            public Throwable cause() {
                return cause;
            }

            @Override
            public boolean succeeded() {
                return cause == null;
            }

            @Override
            public boolean failed() {
                return cause != null;
            }
        };
    }
}
