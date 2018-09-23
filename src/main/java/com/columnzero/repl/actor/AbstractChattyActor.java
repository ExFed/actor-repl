package com.columnzero.repl.actor;

import akka.actor.AbstractLoggingActor;

public abstract class AbstractChattyActor extends AbstractLoggingActor {

    @Override
    public void preStart() {
        log().info("Starting: {}", getSelf());
    }

    @Override
    public void postStop() {
        log().info("Stopped: {}", getSelf());
    }
}
