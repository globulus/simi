package net.globulus.simi.gson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.globulus.simi.SimiMapper;
import net.globulus.simi.api.*;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

@SimiJavaClass(name = "Gson")
public class SimiGson {

    @SimiJavaMethod
    public static SimiProperty parse(SimiObject self, BlockInterpreter interpreter, SimiProperty jsonString) {
        String string = jsonString.getValue().getString();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> map = new Gson().fromJson(string, type);
        return new SimiValue.Object(SimiMapper.toObject(map, true, interpreter));
    }

    @SimiJavaMethod
    public static SimiProperty parseAsync(SimiObject self, BlockInterpreter interpreter, SimiProperty jsonString, SimiProperty callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String string = jsonString.getValue().getString();
                Type type = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String, Object> map = new Gson().fromJson(string, type);
                callback.getValue().getCallable().call(interpreter, Collections.singletonList(new SimiValue.Object(SimiMapper.toObject(map, true, interpreter))), false);
            }
        }).start();
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty stringify(SimiObject self, BlockInterpreter interpreter, SimiProperty object) {
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> map = SimiMapper.fromObject(object.getValue().getObject());
        String jsonString = new Gson().toJson(map, type);
        return new SimiValue.String(jsonString);
    }
}
