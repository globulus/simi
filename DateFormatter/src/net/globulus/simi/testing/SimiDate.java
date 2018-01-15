package net.globulus.simi.testing;

import net.globulus.simi.api.*;

import java.text.SimpleDateFormat;

@SimiJavaClass(name = "Date")
public class SimiDate {

    @SimiJavaMethod
    public static SimiValue format(SimiObject self, SimiEnvironment environment, SimiValue pattern) {
        long timestamp = self.get("timestamp", environment).getNumber().longValue();
        return new SimiValue.String(new SimpleDateFormat(pattern.getString()).format(new java.util.Date(timestamp)));
    }
}
