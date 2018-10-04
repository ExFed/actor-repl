package com.columnzero.repl.message;

import akka.actor.ActorContext;

public interface Synchronized {

    void acknowledge(ActorContext context);
}
