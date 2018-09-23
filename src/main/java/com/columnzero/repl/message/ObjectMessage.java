package com.columnzero.repl.message;

public class ObjectMessage {
    private final Object body;

    public ObjectMessage(Object body) {
        this.body = body;
    }

    public Object getBody() {
        return body;
    }
}
