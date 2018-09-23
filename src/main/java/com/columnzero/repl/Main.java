package com.columnzero.repl;

import akka.actor.ActorSystem;
import com.columnzero.repl.actor.ReplSupervisor;
import org.apache.commons.lang3.text.WordUtils;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class Main {

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create();
        system.actorOf(ReplSupervisor.props(Collections.singletonList(Main::capitalizeFully)));

        try {
            system.getWhenTerminated().toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException e) {
            system.terminate();
        }
    }

    private static Object capitalizeFully(Object p) throws Exception {
        final String str = String.valueOf(p);
        if ("error".equals(str)) {
            throw new Exception("bad error!");
        }
        return WordUtils.capitalizeFully(str);
    }
}
