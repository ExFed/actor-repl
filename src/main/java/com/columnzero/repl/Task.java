package com.columnzero.repl;

@FunctionalInterface
public interface Task<P, R> {
    R execute(P parameter) throws Exception;
}
