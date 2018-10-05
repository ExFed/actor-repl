package com.columnzero.repl.message.signal;

import akka.actor.ActorRef;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Subscribe extends Signal<ActorRef> {

    private final Set<String> topics;

    public Subscribe(ActorRef subscriber, Set<String> topics) {
        super(subscriber);
        this.topics = new HashSet<>(topics);
    }

    public Subscribe(ActorRef subscriber) {
        this(subscriber, Collections.emptySet());
    }

    public ActorRef getSubscriber() {
        return getBody();
    }

    public Set<String> getTopics() {
        return Collections.unmodifiableSet(topics);
    }
}
