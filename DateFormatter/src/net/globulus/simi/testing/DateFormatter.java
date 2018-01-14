package net.globulus.simi.testing;

import net.globulus.simi.api.*;

import java.text.SimpleDateFormat;
import java.util.Date;

@SimiJavaClass
public class DateFormatter {

    @SimiJavaMethod
    public static SimiValue format(SimiObject self, SimiEnvironment environment, SimiValue pattern) {
        long timestamp = self.get("timestamo", environment).getNumber().longValue();
        return new SimiValue.String(new SimpleDateFormat(pattern.getString()).format(new Date(timestamp)));
    }
}
