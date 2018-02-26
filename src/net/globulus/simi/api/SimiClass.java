package net.globulus.simi.api;

import java.util.List;

public interface SimiClass extends SimiObject {
    SimiProperty init(BlockInterpreter interpreter, List<SimiProperty> args);
}
