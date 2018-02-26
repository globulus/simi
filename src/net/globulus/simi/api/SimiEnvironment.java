package net.globulus.simi.api;

public interface SimiEnvironment {
    void define(String name, SimiProperty property);
    SimiProperty tryGet(String name);
}
