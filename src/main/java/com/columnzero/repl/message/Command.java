package com.columnzero.repl.message;

import akka.actor.ActorRef;

public class Command<T> extends DataMessage<T> {

    public static Ready ready() {
        return new Ready();
    }

    public static Shutdown shutdown() {
        return new Shutdown();
    }

    public static Subscribe subscribe(ActorRef subscriber) {
        return new Subscribe(subscriber);
    }

    public Command(T body) {
        super(body);
    }

    public Command() {
        super(null);
    }

    public static class Ready extends Command<Void> {

    }

    public static class Shutdown extends Command<Void> {

    }

    public static class Subscribe extends Command<ActorRef> {

        private Subscribe(ActorRef subscriber) {
            super(subscriber);
        }

        public ActorRef getSubscriber() {
            return getBody();
        }
    }
}
