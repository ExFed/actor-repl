package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.Props;
import com.columnzero.repl.Task;
import com.columnzero.repl.message.Command;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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

    private ReplSupervisor(List<Task<? super Object, ?>> tasks) {

        final ActorRef inputPublisher = context().actorOf(InputPublisher.props(self()), "input-publisher");
        final ActorRef outPrinter = context().actorOf(PrintActor.stdOutProps(inputPublisher), "output-printer");
        final ActorRef errPrinter = context().actorOf(PrintActor.stdErrProps(inputPublisher), "error-printer");

        final LinkedList<ActorRef> taskActors = Lists.reverse(tasks).stream()
                .collect(LinkedList::new,
                         (actors, task) -> accumulateTasks(actors, task, outPrinter, errPrinter, context()),
                         LinkedList::addAll);
        inputPublisher.tell(Command.subscribe(taskActors.peek()), self());
        inputPublisher.tell(Command.ready(), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.Shutdown.class, cmd -> shutdown())
                .match(Command.class, this::onCommand)
                .build();
    }

    private void onCommand(Command command) {
        final String commandBody = command.getBody();
        if (SHUTDOWN_KEYWORDS.contains(commandBody)) {
            shutdown();
            return;
        }

        log().info("Command received: {}", commandBody);
        context().sender().tell(Command.ready(), self());
    }

    private void shutdown() {
        context().system().terminate();
    }
}
