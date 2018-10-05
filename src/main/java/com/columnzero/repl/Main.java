package com.columnzero.repl;

import akka.actor.ActorSystem;
import com.columnzero.repl.actor.ReplSupervisor;
import org.apache.commons.text.WordUtils;

import java.util.Collections;

public class Main {

    public static void main(String[] args) {
        final ActorSystem system = ActorSystem.create("repl");
        system.actorOf(ReplSupervisor.props(Collections.singletonList(Main::capitalizeFully)));
    }

    private static Object capitalizeFully(Object p) throws Exception {
        final String str = String.valueOf(p);
        if ("error".equals(str)) {
            throw new Exception("bad error!");
        }
        return WordUtils.capitalizeFully(str);
    }
}
