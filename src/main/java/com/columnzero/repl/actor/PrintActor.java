package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.columnzero.repl.message.Command;

import java.io.PrintStream;

public class PrintActor extends AbstractChattyActor {

    public static Props stdOutProps(ActorRef notifyComplete) {
        return Props.create(PrintActor.class, () -> new PrintActor(System.out, notifyComplete));
    }

    public static Props stdErrProps(ActorRef notifyComplete) {
        return Props.create(PrintActor.class, () -> new PrintActor(System.err, notifyComplete));
    }

    private final PrintStream stream;
    private final ActorRef notifyComplete;

    private PrintActor(PrintStream stream, ActorRef notifyComplete) {
        this.stream = stream;
        this.notifyComplete = notifyComplete;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(this::onReceive)
                .build();
    }

    private void onReceive(Object o) {
        stream.println(String.valueOf(o));
        notifyComplete.tell(Command.ready(), self());
    }
}
