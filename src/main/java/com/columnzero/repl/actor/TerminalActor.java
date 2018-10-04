package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.columnzero.repl.message.Command;
import com.columnzero.repl.message.DataMessage;
import com.columnzero.repl.message.ErrorMessage;
import com.columnzero.repl.message.TrackedMessage;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class TerminalActor extends AbstractChattyActor {

    private static final String COMMAND_PREFIX = "cmd:";

    public static Props props(ActorRef supervisor) {
        return Props.create(TerminalActor.class,
                            () -> new TerminalActor(supervisor, System.in, System.out, System.err));
    }

    private final ActorRef supervisor;
    private final Scanner inScanner;
    private final PrintStream out;
    private final PrintStream err;
    private final Set<ActorRef> subscribers = new HashSet<>();

    private TerminalActor(ActorRef supervisor, InputStream in, PrintStream out, PrintStream err) {
        this.supervisor = supervisor;
        this.inScanner = new Scanner(in);
        this.out = out;
        this.err = err;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.Ready.class, ready -> scanForInput())
                .match(Command.Subscribe.class, this::addSubscriber)
                .match(ErrorMessage.class, this::printError)
                .matchAny(this::printAny)
                .build();
    }

    private void addSubscriber(Command.Subscribe subscribeCommand) {
        final ActorRef subscriber = subscribeCommand.getSubscriber();
        log().info("Adding subscriber: {}", subscriber);
        subscribers.add(subscriber);
    }

    private void printAny(Object o) {
        out.println(String.valueOf(o));

        if (o instanceof TrackedMessage) {
            final TrackedMessage<?> syn = (TrackedMessage<?>) o;
            syn.acknowledge(context());
        }
    }

    private void printError(ErrorMessage<?> o) {
        err.println(o.getBody());
    }

    private void scanForInput() {
        System.out.print("> "); // prompt
        final String line = inScanner.nextLine();

        handleInput(line);
    }

    private void handleInput(String line) {
        if (StringUtils.startsWith(line, COMMAND_PREFIX)) {
            final String cmd = StringUtils.removeStart(line, COMMAND_PREFIX);
            supervisor.tell(new Command<>(cmd), self());
        } else {
            tellSubscribers(new DataMessage<>(line));
        }
    }

    private void tellSubscribers(Object message) {
        for (ActorRef subscriber : subscribers) {
            subscriber.tell(message, self());
        }
    }
}
