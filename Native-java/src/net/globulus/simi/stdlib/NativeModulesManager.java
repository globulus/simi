package net.globulus.simi.stdlib;

import net.globulus.simi.api.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SimiJavaClass
public class NativeModulesManager {

    private static final String API_CLASS = Constants.PACKAGE_SIMI_API + "." + Constants.API_CLASS_NAME;

    @SimiJavaMethod
    public static SimiValue init(SimiObject self, BlockInterpreter interpreter, SimiValue file) {
        SimiClass clazz = (SimiClass) self;
        Wrapper wrapper = new Wrapper();
        LinkedHashMap<String, SimiValue> props = new LinkedHashMap<>();
        props.put("native_wrapper", new SimiValue.Object(wrapper));
        SimiObject object = interpreter.newInstance(clazz, props);
        return new SimiValue.Object(object);
    }

    @SimiJavaMethod
    public static SimiValue loadJar(SimiObject self, BlockInterpreter interpreter, SimiValue path) {
        URL url = null;
        try {
            url = Paths.get(path.getString()).toUri().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        ClassLoader loader = URLClassLoader.newInstance(
                new URL[] { url },
                self.getClass().getClassLoader()
        );
        Wrapper wrapper = getWrapper(self, interpreter);
        try {
            SimiApiClass apiClass = (SimiApiClass) Class.forName(API_CLASS, true, loader).newInstance();
            for (String className : apiClass.classNames()) {
                wrapper.classes.put(className, apiClass);
            }
            for (String globalMethodName : apiClass.globalMethodNames()) {
                wrapper.globals.put(globalMethodName, apiClass);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    @SimiJavaMethod
    public static SimiValue call(SimiObject self,
                                 BlockInterpreter interpreter,
                                 SimiValue className,
                                 SimiValue methodName,
                                 SimiValue args) throws IllegalArgumentException {
        Wrapper wrapper = getWrapper(self, interpreter);
        String classNameStr = className.getString();
        if (classNameStr.equals(Constants.GLOBALS_CLASS_NAME)) {
            SimiApiClass apiClass = wrapper.globals.get(methodName.getString());
            if (apiClass != null) {
                return apiClass.call(classNameStr, methodName.getString(), self, interpreter, args.getObject().values());
            }
        } else {
            SimiApiClass apiClass = wrapper.classes.get(classNameStr);
            if (apiClass != null) {
                return apiClass.call(classNameStr, methodName.getString(), self, interpreter, args.getObject().values());
            }
        }
        throw new IllegalArgumentException();
    }

    private static Wrapper getWrapper(SimiObject self, BlockInterpreter interpreter) {
        return (Wrapper) self.get("native_wrapper", interpreter.getEnvironment()).getObject();
    }

    private static class Wrapper implements SimiObject {

        Map<String, SimiApiClass> classes;
        Map<String, SimiApiClass> globals;

        Wrapper() {
            classes = new HashMap<>();
            globals = new HashMap<>();
        }

        @Override
        public SimiClass getSimiClass() {
            return null;
        }

        @Override
        public SimiValue get(String s, SimiEnvironment simiEnvironment) {
            return null;
        }

        @Override
        public void set(String s, SimiValue simiValue, SimiEnvironment simiEnvironment) {

        }

        @Override
        public SimiObject clone(boolean b) {
            return null;
        }

        @Override
        public List<SimiValue> values() {
            return null;
        }
    }
}
