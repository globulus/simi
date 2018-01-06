package net.globulus.simi.api;

public interface BlockInterpreter {

    void executeBlock(SimiBlock block, SimiEnvironment environment);
    SimiValue getGlobal(String name);
    SimiEnvironment getEnvironment();
}
