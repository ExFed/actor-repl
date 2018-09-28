package com.columnzero.repl.message;

import akka.actor.ActorContext;
import akka.actor.ActorRef;

public class SynchronizedMessage<T> extends DataMessage<T> {

    private final int id;
    private final ActorRef originator;

    public SynchronizedMessage(int id, ActorRef originator, T body) {
        super(body);

        this.id = id;
        this.originator = originator;
    }

    public int getId() {
        return id;
    }

    public ActorRef getOriginator() {
        return originator;
    }

    public <V> SynchronizedMessage<V> transform(V newValue) {
        return new SynchronizedMessage<>(id, originator, newValue);
    }

    public void acknowledge(ActorContext context) {
        originator.tell(new Acknowledgement(id), context.self());
    }
}
