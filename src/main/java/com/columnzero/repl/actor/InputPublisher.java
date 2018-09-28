package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.columnzero.repl.message.Acknowledgement;
import com.columnzero.repl.message.Command;
import com.columnzero.repl.message.SynchronizedMessage;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;

public class InputPublisher extends AbstractChattyActor {

    public static final String COMMAND_PREFIX = "cmd:";

    public static Props props(ActorRef supervisor) {
        return Props.create(InputPublisher.class, () -> new InputPublisher(supervisor));
    }

    private final Scanner inputScanner = new Scanner(System.in);
    private final Set<ActorRef> subscribers = new HashSet<>();
    private final ActorRef supervisor;

    private int inputId = 0;

    public InputPublisher(ActorRef supervisor) {
        this.supervisor = supervisor;

        getContext().getParent().tell(Command.ready(), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.Ready.class, this::startScanningForInput)
                .match(Acknowledgement.class, this::resumeScanningForInput)
                .match(Command.Subscribe.class, this::addSubscriber)
                .build();
    }

    private void addSubscriber(Command.Subscribe subscribeCommand) {
        final ActorRef subscriber = subscribeCommand.getSubscriber();
        log().info("Adding subscriber: {}", subscriber);
        subscribers.add(subscriber);
    }

    private void startScanningForInput(Command.Ready ready) {
        if (inputId == 0) {
            scanForInput();
        }
    }

    private void resumeScanningForInput(Acknowledgement ack) {
        if (Objects.equals(ack.getId(), inputId)) {
            inputId++;
            scanForInput();
        }
    }

    private void scanForInput() {
        System.out.print("> "); // prompt
        final String line = inputScanner.nextLine();

        handleInput(line);
    }

    private void handleInput(String line) {
        if (StringUtils.startsWith(line, COMMAND_PREFIX)) {
            final String cmd = StringUtils.removeStart(line, COMMAND_PREFIX);
            supervisor.tell(new SynchronizedMessage<>(inputId, self(), cmd), self());
        } else {
            tellSubscribers(new SynchronizedMessage<>(inputId, self(), line));
        }
    }

    private void tellSubscribers(Object message) {
        for (ActorRef subscriber : subscribers) {
            subscriber.tell(message, self());
        }
    }
}
