package net.globulus.simi.api;

public interface SimiEnvironment {
    void define(String name, SimiValue value);
    SimiValue tryGet(String name);
}
