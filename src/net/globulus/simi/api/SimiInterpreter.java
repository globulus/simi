package net.globulus.simi.api;

public interface SimiInterpreter {

    void executeBlock(SimiBlock block, SimiEnvironment environment);
}
