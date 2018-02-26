package net.globulus.simi.api;

import java.util.List;

public interface SimiApiClass {
    SimiProperty call(String className,
                   String methodName,
                   SimiObject self,
                   BlockInterpreter interpreter,
                   List<SimiProperty> args);
    String[] classNames();
    String[] globalMethodNames();
}
