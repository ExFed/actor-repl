package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.Props;
import com.columnzero.repl.Task;
import com.columnzero.repl.message.signal.Signal;
import com.columnzero.repl.message.signal.Ready;
import com.columnzero.repl.message.signal.Shutdown;
import com.columnzero.repl.message.signal.Subscribe;
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
                                        ActorRef output,
                                        ActorRefFactory factory) {
        final Props props;
        if (actors.isEmpty()) {
            props = TaskActor.props(output, output, task);
        } else {
            props = TaskActor.props(actors.peek(), output, task);
        }
        actors.push(factory.actorOf(props, "task-" + actors.size()));
    }

    private final Set<ActorRef> unreadyChildren = new HashSet<>();

    private ReplSupervisor(List<Task<? super Object, ?>> tasks) {

        final ActorRef console = context().actorOf(TerminalActor.props(self()), "console");

        final LinkedList<ActorRef> taskActors = Lists.reverse(tasks).stream()
                .collect(LinkedList::new,
                         (actors, task) -> accumulateTasks(actors, task, console, context()),
                         LinkedList::addAll);
        console.tell(new Subscribe(taskActors.peek()), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Ready.class, cmd -> onReady())
                .match(Shutdown.class, cmd -> shutdown())
                .match(Signal.class, this::onMessage)
                .build();
    }

    private void onMessage(Signal<?> cmd) {
        final String body = String.valueOf(cmd.getBody());
        if (SHUTDOWN_KEYWORDS.contains(body)) {
            shutdown();
            return;
        }

        log().info("Signal received: {}", body);
    }

    private void onReady() {
        unreadyChildren.remove(sender());
        if (unreadyChildren.isEmpty()) {
            for (ActorRef child : getContext().getChildren()) {
                child.tell(Signal.ready(), self());
            }
        }
    }

    private void shutdown() {
        context().system().terminate();
    }
}
