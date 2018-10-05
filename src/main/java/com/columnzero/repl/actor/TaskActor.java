package com.columnzero.repl.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.columnzero.repl.Task;
import com.columnzero.repl.message.signal.Signal;
import com.columnzero.repl.message.Message;
import com.columnzero.repl.message.TrackedMessage;
import com.columnzero.repl.message.signal.Shutdown;
import com.columnzero.repl.message.signal.Subscribe;

import java.util.Collections;
import java.util.Set;

public class TaskActor extends AbstractChattyActor {

    private static final Set<String> OUT_TOPIC = Collections.singleton("out");
    private static final Set<String> ERR_TOPIC = Collections.singleton("err");

    public static Props props(ActorRef next, ActorRef error, Task<? super Object, ?> task) {
        final Set<ActorRef> nextActors = Collections.singleton(next);
        final Set<ActorRef> errorActors = Collections.singleton(error);
        return Props.create(TaskActor.class, () -> new TaskActor(nextActors, errorActors, task));
    }

    public static Subscribe writeSubscription(ActorRef subscriber, boolean errorTopic) {
        return new Subscribe(subscriber, errorTopic ? ERR_TOPIC : OUT_TOPIC);
    }

    private final Set<ActorRef> outSubs;
    private final Set<ActorRef> errSubs;

    private final Task<? super Object, ?> task;

    private TaskActor(Set<ActorRef> outSubs, Set<ActorRef> errSubs, Task<? super Object, ?> task) {
        this.outSubs = outSubs;
        this.errSubs = errSubs;
        this.task = task;

        getContext().getParent().tell(Signal.ready(), self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Shutdown.class, this::shutdown)
                .match(Subscribe.class, this::onSubscribe)
                .match(Message.class, this::onReceive)
                .build();
    }

    private void onSubscribe(Subscribe subscribe) {
        if (subscribe.getTopics().contains("out")) {
            outSubs.add(subscribe.getSubscriber());
        }

        if (subscribe.getTopics().contains("err")) {
            errSubs.add(subscribe.getSubscriber());
        }
    }

    private void shutdown(Shutdown command) {
        context().stop(getSelf());
    }

    private void onReceive(Message<?> message) {
        try {

            final Object result;
            if (message instanceof TrackedMessage) {
                final TrackedMessage<?> syn = (TrackedMessage<?>) message;
                result = syn.transform(getSelf(), task.execute(message.getBody()));
            } else {
                result = task.execute(message.getBody());
            }
            tellAll(outSubs, result);
        } catch (Exception e) {
            tellAll(errSubs, e);
        }
    }

    private void tellAll(Set<ActorRef> actorRefs, Object result) {
        for (ActorRef ar : actorRefs) {
            ar.tell(result, getSelf());
        }
    }
}
