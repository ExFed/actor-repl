package com.columnzero.repl.message.signal;

public class Ready extends Signal<Void> {

    public static final Ready READY = new Ready();

    @Override
    public String toString() {
        return "Ready";
    }
}
