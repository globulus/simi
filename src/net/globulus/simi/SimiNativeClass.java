package net.globulus.simi;

import net.globulus.simi.api.*;

import java.util.Map;

class SimiNativeClass {

    final String name;
    final Map<OverloadableFunction, SimiCallable> methods;

    SimiNativeClass(String name, Map<OverloadableFunction, SimiCallable> methods) {
        this.name = name;
        this.methods = methods;
    }
}
