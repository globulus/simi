package net.globulus.simi.api;

import java.util.List;

public interface SimiApiClass {
    SimiValue call(String className,
                   String methodName,
                   SimiObject self,
                   BlockInterpreter interpreter,
                   List<SimiValue> args);
    String[] classNames();
    String[] globalMethodNames();
}
