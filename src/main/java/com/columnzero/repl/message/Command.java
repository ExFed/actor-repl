package com.columnzero.repl.message;

import akka.actor.ActorRef;

public class Command {

    private final String body;

    public Command(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public static Ready ready() {
        return new Ready();
    }

    public static Shutdown shutdown() {
        return new Shutdown();
    }

    public static Subscribe subscribe(ActorRef subscriber) {
        return new Subscribe(subscriber);
    }

    public static class Ready {

    }

    public static class Shutdown {

    }

    public static class Subscribe {

        private final ActorRef subscriber;

        private Subscribe(ActorRef subscriber) {
            this.subscriber = subscriber;
        }
        public ActorRef getSubscriber() {
            return subscriber;
        }
    }
}
