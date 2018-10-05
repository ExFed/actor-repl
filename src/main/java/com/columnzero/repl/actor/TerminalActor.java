package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.columnzero.repl.message.DataMessage;
import com.columnzero.repl.message.ErrorMessage;
import com.columnzero.repl.message.signal.Ready;
import com.columnzero.repl.message.signal.Signal;
import com.columnzero.repl.message.signal.Subscribe;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class TerminalActor extends AbstractChattyActor {

    private static final ReadNext READ_NEXT = new ReadNext();
    private static final String COMMAND_PREFIX = "cmd:";
    private static final String MESSAGE_PREFIX = "msg:";

    public static Props props() {
        return Props.create(TerminalActor.class, () -> new TerminalActor(System.in, System.out, System.err));
    }

    private final Scanner inScanner;
    private final PrintStream out;
    private final PrintStream err;
    private final Set<ActorRef> subscribers = new HashSet<>();

    private boolean ready = false;

    private TerminalActor(InputStream in, PrintStream out, PrintStream err) {
        this.inScanner = new Scanner(in);
        this.out = out;
        this.err = err;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Ready.class, () -> sender().equals(context().parent()), this::startLoop)
                .match(ReadNext.class, this::scanForInput)
                .match(Subscribe.class, this::addSubscriber)
                .match(DataMessage.class, this::printOut)
                .match(ErrorMessage.class, this::printError)
                .matchAny(this::printOut)
                .build();
    }

    private void addSubscriber(Subscribe subscribeCommand) {
        final ActorRef subscriber = subscribeCommand.getSubscriber();
        log().info("Adding subscriber: {}", subscriber);
        subscribers.add(subscriber);
    }

    private void printOut(DataMessage o) {
        out.println(String.valueOf(o.getBody()));
    }

    private void printOut(Object o) {
        out.println(getSender());
        out.println(String.valueOf(o));
    }

    private void printError(ErrorMessage<?> o) {
        err.println(o.getBody());
    }

    private void startLoop(Object obj) {
        if (!ready) {
            ready = true;
            readNext();
        }
    }

    private void scanForInput(Object obj) {
        System.out.print("> "); // prompt
        final String line = inScanner.nextLine();

        handleInput(line);
        readNext(); // once the input buffer flushes, repeat
    }

    private void readNext() {
        self().tell(READ_NEXT, getSelf());
    }

    private void handleInput(String line) {

        if (StringUtils.startsWith(line, COMMAND_PREFIX)) {
            final String cmd = StringUtils.removeStart(line, COMMAND_PREFIX);
            context().parent().tell(new Signal<>(cmd), self());
        } else if (StringUtils.startsWith(line, MESSAGE_PREFIX)) {
            final String msg = StringUtils.removeStart(line, MESSAGE_PREFIX);
            tellSubscribers(new DataMessage<>(msg));
        }
    }

    private void tellSubscribers(Object message) {
        for (ActorRef subscriber : subscribers) {
            subscriber.tell(message, self());
        }
    }

    private static class ReadNext {}
}
