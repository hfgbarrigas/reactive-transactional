package io.hfgbarrigas.reactive.dbcontext.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Indicates that a database connection must be created or reused within method execution.
 * All underlying database accesses will reuse the connection.
 * <p>
 *      The annotation can be used in the following targets:
 *      <ul>
 *          <li><em>class</em></li>
 *          <li><em>method</em></li>
 *      </ul>
 * </p>
 *
 * <p>
 *      The connection type:
 *      <ul>
 *          <li>{@link TransactionalContextType#READ}</li>: a connection is to be extracted from the pool. DEFAULT
 *          <li>{@link TransactionalContextType#WRITE}</li>: a connection is to be extracted from pool and a transaction started
 *      </ul>
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface TransactionalContext {
    TransactionalContextType value() default TransactionalContextType.READ;
}
