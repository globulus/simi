package net.globulus.simi.stdlib;

import net.globulus.simi.api.*;

import java.text.SimpleDateFormat;
import java.util.Collections;

@SimiJavaClass(name = "Date")
public class SimiDate {

    @SimiJavaMethod
    public static SimiValue now(SimiObject self, BlockInterpreter interpreter) {
        SimiClass clazz = (SimiClass) self;
        SimiValue timestamp = new SimiValue.Number(System.currentTimeMillis());
        return clazz.init(interpreter, Collections.singletonList(timestamp));
    }

    @SimiJavaMethod
    public static SimiValue format(SimiObject self, BlockInterpreter interpreter, SimiValue pattern) {
        long timestamp = self.get("timestamp", interpreter.getEnvironment()).getNumber().longValue();
        return new SimiValue.String(new SimpleDateFormat(pattern.getString()).format(new java.util.Date(timestamp)));
    }
}
