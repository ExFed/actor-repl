package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.columnzero.repl.Task;
import com.columnzero.repl.message.Command;
import com.columnzero.repl.message.ObjectMessage;

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
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.class, this::onCommand)
                .match(ObjectMessage.class, this::onReceive)
                .build();
    }

    private void onCommand(Command command) {

        final String cmd = command.getBody();
        switch (cmd) {
            case "shutdown":
                log().info("Stopping: {}", getSelf());
                context().stop(getSelf());
            default:
                log().info("Received command: {}", cmd);
        }
    }

    private void onReceive(ObjectMessage message) {
        try {
            success.tell(task.execute(message.getBody()), getSelf());
        } catch (Exception e) {
            error.tell(e, getSelf());
        }
    }

}
