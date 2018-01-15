package net.globulus.simi.api;

import java.util.List;

public interface SimiApiClass {
    SimiValue call(String className,
                   String methodName,
                   SimiObject self,
                   SimiEnvironment environment,
                   List<SimiValue> args);
    String[] classNames();
    String[] globalMethodNames();
}
