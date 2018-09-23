package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.columnzero.repl.message.Command;
import com.columnzero.repl.message.ObjectMessage;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class InputPublisher extends AbstractChattyActor {

    public static final String COMMAND_PREFIX = "cmd:";

    public static Props props(ActorRef supervisor) {
        return Props.create(InputPublisher.class, () -> new InputPublisher(supervisor));
    }

    private final Scanner inputScanner = new Scanner(System.in);
    private final Set<ActorRef> subscribers = new HashSet<>();
    private final ActorRef supervisor;

    public InputPublisher(ActorRef supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.Ready.class, this::scanForInput)
                .match(Command.Subscribe.class, this::addSubscriber)
                .build();
    }

    private void addSubscriber(Command.Subscribe subscribeCommand) {
        final ActorRef subscriber = subscribeCommand.getSubscriber();
        log().info("Adding subscriber: {}", subscriber);
        subscribers.add(subscriber);
    }

    private void scanForInput(Command.Ready ignore) {
        System.out.print("> ");
        final String line = inputScanner.nextLine();

        if (StringUtils.startsWith(line, COMMAND_PREFIX)) {
            final String cmdBody = StringUtils.removeStart(line, COMMAND_PREFIX);
            supervisor.tell(new Command(cmdBody), self());
        } else {
            tellSubscribers(new ObjectMessage(line));
        }
    }

    private void tellSubscribers(Object message) {
        for (ActorRef subscriber : subscribers) {
            subscriber.tell(message, self());
        }
    }
}
