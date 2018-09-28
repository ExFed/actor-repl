package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.Props;
import com.columnzero.repl.Task;
import com.columnzero.repl.message.Command;
import com.columnzero.repl.message.SynchronizedMessage;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ReplSupervisor extends AbstractChattyActor {

    private static final Set<String> SHUTDOWN_KEYWORDS = Sets.newHashSet("exit", "quit", "shutdown", "terminate");

    public static Props props(List<Task<? super Object, ?>> tasks) {

        return Props.create(ReplSupervisor.class, () -> new ReplSupervisor(tasks));
    }

    private static void accumulateTasks(LinkedList<ActorRef> actors,
                                        Task<? super Object, ?> task,
                                        ActorRef outActor,
                                        ActorRef errActor,
                                        ActorRefFactory factory) {
        final Props props;
        if (actors.isEmpty()) {
            props = TaskActor.props(outActor, errActor, task);
        } else {
            props = TaskActor.props(actors.peek(), errActor, task);
        }
        actors.push(factory.actorOf(props, "task-" + actors.size()));
    }

    private final Set<ActorRef> unreadyChildren = new HashSet<>();

    private ReplSupervisor(List<Task<? super Object, ?>> tasks) {

        final ActorRef inputPublisher = context().actorOf(InputPublisher.props(self()), "input-publisher");
        final ActorRef outPrinter = context().actorOf(PrintActor.stdOutProps(), "output-printer");
        final ActorRef errPrinter = context().actorOf(PrintActor.stdErrProps(), "error-printer");

        final LinkedList<ActorRef> taskActors = Lists.reverse(tasks).stream()
                .collect(LinkedList::new,
                         (actors, task) -> accumulateTasks(actors, task, outPrinter, errPrinter, context()),
                         LinkedList::addAll);
        inputPublisher.tell(Command.subscribe(taskActors.peek()), self());

        unreadyChildren.add(inputPublisher);
        unreadyChildren.add(outPrinter);
        unreadyChildren.add(errPrinter);
        unreadyChildren.addAll(taskActors);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.Ready.class, cmd -> onReady())
                .match(Command.Shutdown.class, cmd -> shutdown())
                .match(SynchronizedMessage.class, this::onMessage)
                .build();
    }



    private void onMessage(SynchronizedMessage<?> message) {
        final String body = String.valueOf(message.getBody());
        if (SHUTDOWN_KEYWORDS.contains(body)) {
            shutdown();
            return;
        }

        log().info("Command received: {}", body);
        message.acknowledge(context());
    }

    private void onReady() {
        unreadyChildren.remove(sender());
        if (unreadyChildren.isEmpty()) {
            for (ActorRef child : getContext().getChildren()) {
                child.tell(Command.ready(), self());
            }
        }
    }

    private void shutdown() {
        context().system().terminate();
    }
}
