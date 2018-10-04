package com.columnzero.repl.actor;

import akka.actor.Props;
import com.columnzero.repl.message.Synchronized;
import com.columnzero.repl.message.Command;

import java.io.PrintStream;

public class PrintActor extends AbstractChattyActor {

    public static Props stdOutProps() {
        return Props.create(PrintActor.class, () -> new PrintActor(System.out));
    }

    public static Props stdErrProps() {
        return Props.create(PrintActor.class, () -> new PrintActor(System.err));
    }

    private final PrintStream stream;

    private PrintActor(PrintStream stream) {
        this.stream = stream;

        getContext().getParent().tell(Command.ready(), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(this::onReceive)
                .build();
    }

    private void onReceive(Object o) {
        stream.println(String.valueOf(o));

        if (o instanceof Synchronized) {
            final Synchronized syn = (Synchronized) o;
            syn.acknowledge(context());
        }
    }
}
