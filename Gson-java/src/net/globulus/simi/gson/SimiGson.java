package net.globulus.simi.gson;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.beans.binding.ObjectExpression;
import net.globulus.simi.SimiMapper;
import net.globulus.simi.api.*;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SimiJavaConfig(apiClassName = "Gson_java")
@SimiJavaClass(name = "Gson")
public class SimiGson {

    @SimiJavaMethod
    public static SimiProperty parse(SimiObject self, BlockInterpreter interpreter, SimiProperty jsonString) {
        String string = jsonString.getValue().getString().trim();
        if (string.startsWith("[")) {
            Type type = new TypeToken<List<Object>>(){}.getType();
            List<Object> list = new Gson().fromJson(string, type);
            return new SimiValue.Object(SimiMapper.toObject(list, true, interpreter));
        }
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> map = new Gson().fromJson(string, type);
        return new SimiValue.Object(SimiMapper.toObject(map, true, interpreter));
    }

    @SimiJavaMethod
    public static SimiProperty parseAsync(SimiObject self, BlockInterpreter interpreter, SimiProperty jsonString, SimiProperty callback) {
        new Thread(() -> {
            callback.getValue().getCallable().call(interpreter, Collections.singletonList(parse(self, interpreter, jsonString)), false);
        }).start();
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty stringify(SimiObject self, BlockInterpreter interpreter, SimiProperty object) {
        SimiValue value = object.getValue();
        if (!(value instanceof SimiValue.Object)) {
            return object;
        }
        Object javaObject = SimiMapper.fromSimiValue(value.getValue());
        String jsonString;
        if (javaObject instanceof Map) {
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> map = (Map<String, Object>) javaObject;
            jsonString = new Gson().toJson(map, type);

        } else {
            jsonString = new Gson().toJson(javaObject);
        }
        return new SimiValue.String(jsonString);
    }
}
