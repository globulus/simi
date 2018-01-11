package net.globulus.simi.api;

public final class SimiException extends RuntimeException {

    public final SimiClass clazz;

    public SimiException(SimiClass clazz, String message) {
        super(message);
        this.clazz = clazz;
    }
}
