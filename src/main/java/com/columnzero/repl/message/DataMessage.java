package com.columnzero.repl.message;

public class DataMessage<T> implements Message<T> {

    private final T body;

    public DataMessage(T body) {
        this.body = body;
    }

    @Override
    public T getBody() {
        return body;
    }


    @Override
    public String toString() {
        return String.valueOf(getBody());
    }
}
