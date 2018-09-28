package com.columnzero.repl.message;

public class Acknowledgement extends DataMessage<Integer> {

    public Acknowledgement(int id) {
        super(id);

    }

    public int getId() {
        return getBody();
    }
}
