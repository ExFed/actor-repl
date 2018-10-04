package com.columnzero.repl.message;

import akka.actor.ActorContext;
import akka.actor.ActorRef;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class TrackedMessage<T> extends DataMessage<T> implements Synchronized {

    private final int id;
    private final List<ActorRef> provenance;

    public TrackedMessage(int id, ActorRef originator, T body) {
        this(id, Collections.singletonList(originator), body);
    }

    private TrackedMessage(int id, List<ActorRef> provenance, T body) {
        super(body);

        this.id = id;
        this.provenance = provenance;
    }

    public int getId() {
        return id;
    }

    public List<ActorRef> getProvenance() {
        return provenance;
    }

    public <R> TrackedMessage<R> transform(ActorRef actor, Function<T, R> transformer) {
        return new TrackedMessage<>(id, provenance, transformer.apply(this.getBody()));
    }

    public <R> TrackedMessage<R> transform(ActorRef actor, R newValue) {
        return new TrackedMessage<>(id, provenance, newValue);
    }

    @Override
    public void acknowledge(ActorContext context) {
        provenance.get(0).tell(new Acknowledgement(id), context.self());
    }
}
