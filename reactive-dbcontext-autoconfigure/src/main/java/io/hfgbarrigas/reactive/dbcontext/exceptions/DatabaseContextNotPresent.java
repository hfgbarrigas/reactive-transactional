package io.hfgbarrigas.reactive.dbcontext.exceptions;

public class DatabaseContextNotPresent extends RuntimeException {
    public DatabaseContextNotPresent(String message) {
        super(message);
    }
}
