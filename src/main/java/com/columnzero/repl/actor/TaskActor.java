package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.columnzero.repl.Task;
import com.columnzero.repl.message.Command;
import com.columnzero.repl.message.Message;
import com.columnzero.repl.message.SynchronizedMessage;

public class TaskActor extends AbstractChattyActor {

    public static Props props(ActorRef next, ActorRef error, Task<? super Object, ?> task) {
        return Props.create(TaskActor.class, () -> new TaskActor(next, error, task));
    }

    private final ActorRef success;
    private final ActorRef error;

    private final Task<? super Object, ?> task;

    private TaskActor(ActorRef success, ActorRef error, Task<? super Object, ?> task) {
        this.success = success;
        this.error = error;
        this.task = task;

        getContext().getParent().tell(Command.ready(), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.Ready.class, () -> false, this::onCommand)
                .match(Command.class, this::onCommand)
                .match(Message.class, this::onReceive)
                .build();
    }

    private void onCommand(Command<?> command) {

        final String cmd = String.valueOf(command.getBody());
        switch (cmd) {
            case "shutdown":
                log().info("Stopping: {}", getSelf());
                context().stop(getSelf());
            default:
                log().info("Received command: {}", command);
        }
    }

    private void onReceive(Message<?> message) {
        try {

            final Object result;
            if (message instanceof SynchronizedMessage) {
                final SynchronizedMessage<?> syn = (SynchronizedMessage<?>) message;
                result = syn.transform(task.execute(message.getBody()));
            } else {
                result = task.execute(message.getBody());
            }
            success.tell(result, getSelf());
        } catch (Exception e) {
            error.tell(e, getSelf());
        }
    }

}
