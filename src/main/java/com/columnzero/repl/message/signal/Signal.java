package com.columnzero.repl.message.signal;

import com.columnzero.repl.message.DataMessage;

public class Signal<T> extends DataMessage<T> {

    public static Ready ready() {
        return new Ready();
    }

    public Signal(T body) {
        super(body);
    }

    Signal() {
        super(null);
    }
}
