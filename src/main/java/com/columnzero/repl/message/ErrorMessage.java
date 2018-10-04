package com.columnzero.repl.message;

public class ErrorMessage<T> extends DataMessage<T> {

    public ErrorMessage(T body) {
        super(body);
    }
}
