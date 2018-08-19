package net.globulus.simi.stdlib;

import net.globulus.simi.api.*;

import java.text.SimpleDateFormat;
import java.util.Collections;

@SimiJavaConfig(apiClassName = "Stdlib_java")
@SimiJavaClass(name = "Date")
public class SimiDate {

    @SimiJavaMethod
    public static SimiProperty now(SimiObject self, BlockInterpreter interpreter) {
        SimiClass clazz = (SimiClass) self;
        SimiValue timestamp = new SimiValue.Number(System.currentTimeMillis());
        return clazz.init(interpreter, Collections.singletonList(timestamp));
    }

    @SimiJavaMethod
    public static SimiProperty format(SimiObject self, BlockInterpreter interpreter, SimiProperty pattern) {
        long timestamp = self.get("timestamp", interpreter.getEnvironment()).getValue().getNumber().asLong();
        return new SimiValue.String(new SimpleDateFormat(pattern.getValue().getString()).format(new java.util.Date(timestamp)));
    }
}
