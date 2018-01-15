package net.globulus.simi.api;

import java.util.List;

public interface SimiClass extends SimiObject {
    SimiValue init(BlockInterpreter interpreter, List<SimiValue> args);
}
