# @TransactionalContext

Aspect that allows a transactional context, containing a database connection that might possibly be transactional, to be shared through reactive streams context.
    
### How does it work? ###

You should annotate any method, or class, with `@TransactionalContext` if you want a database connection to be created
 in the context. E.g: 

    @TransactionalContext
    public Mono<MyPojo> myMethod()

In the example, any method call within `myMethod()` execution will have a transactional context present in the pipeline context.
It's accessible through `ReactiveTransactionalContextHolder.getContext()`.

By default, `TransactionalContextType.READ` is used, meaning that the connection created will not start a transaction.
If a transaction is to be started, use `TransactionalContextType.WRITE`, where a transaction should be started when the connection is created.

When `myMethod()` finishes executing, the connection will be closed and the transaction commited or rolledback depending on
which signal was received:

    CANCEL - connection will be closed and tx rolledback
    ERROR - connection will be closed and tx rolledback
    SUCCESS - connection will be closed and tx commited

If an error occurs when commiting the transaction, the transaction will be rolledback and `myMethod` will throw an error.

In case multiple methods are chained and all of them are annotated with `@TransactionalContext`, only the first one
will create the connection and the transaction (in case of `WRITE`). Subsequent methods will share the same connection and transaction
when calling `ReactiveTransanctionalContextHolder.getContext()`.
In this scenario, only when `myMethod` finishes executing will the connection be closed and the transaction commited/rolledback.

`@TransactionalContext` can either be used on methods or classes. In the latter, all methods in the class will be affected.
Also, only methods that have return type `reactor.core.publisher.Mono` or `reactor.core.publisher.Flux` are eligible to be affected.

*Important:* When calling `ReactiveTransactionalContext.getContext()` and no transactional context is present, an exception is thrown. 

### How do I use it? ###

To use the aspect there are two requirements, to provide an implementation of ConnectionManager and Connection. Simple interfaces that aim to
abstract the aspect of any specific implementation of the underlying database driver used.

`ConnectionManager`: aims to fetch connections from the database, considering if the connection should or should not be transactional.

`Connection`: Reference to a connection present in the context, can either Commit, Rollback and Close the connection.

This aspect ships with an implementation for postgres - [reactive-pg](https://reactiverse.io/). To override the default configuration,
just declare a bean of type `ConnectionManager`. Check the package `pg` for an example of a concrete implementation.

I'll eventually uploaded it to maven central but for now, fork it and build it locally with: `mvn clean install`

Add the following dependency to your pom.xml.

    <dependency>
        <groupId>io.hfbarrigas</groupId>
        <artifactId>reactive-dbcontext-starter</artifactId>
        <version>X</version>
        <type>pom</type>
    </dependency>
    
### Examples

Consider the following method located in a service that requires all methods to be executed within the same transaction: 

```

@TransactionalContext(TransactionalContextType.WRITE)
public Mono<?> doSomething() {
    return myRepository.save(x, y, z)
        .flatMap(saved -> doSomethingElse(saved))
        .flatMap(someData -> myRepository.update(saved.id, someData))
}


```

`Repositories`

```

@TransactionalContext
class MyRepository {

    public Mono<?> save() {
        return ReactiveTransactionalContext.getContext()
            .map(dbContext -> ((PgConnectionAdapter) dbContext.getConnection()).getConnection)
            .flatMap(pgConnection -> pgConnection.executeQuery("insert into myTable values (x, y, z)"));
    }
    
    public Mono<?> update(id, someData) {
            return ReactiveTransactionalContext.getContext()
                .map(dbContext -> ((PgConnectionAdapter) dbContext.getConnection()).getConnection)
                .flatMap(pgConnection -> pgConnection.executeQuery("update myTable set c1 = someData where id = 1"));
    }
}

```

### TODO
* Move spring autoconfiguration for reactive-pg driver currently used to a different project.

### Who do I talk to? ###

* Hugo Barrigas - hugo.barrigas@mindera.com
