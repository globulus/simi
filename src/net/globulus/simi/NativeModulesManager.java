package net.globulus.simi;

import net.globulus.simi.api.*;
import net.globulus.simi.api.Constants;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NativeModulesManager {

    private static final String API_CLASS = Constants.PACKAGE_SIMI_API + "." + Constants.API_CLASS_NAME;

    private Map<String, SimiApiClass> classes;
    private Map<String, SimiApiClass> globals;

    public NativeModulesManager() {
        classes = new HashMap<>();
        globals = new HashMap<>();
    }

    public void loadJar(URL url) {
        ClassLoader loader = URLClassLoader.newInstance(
                new URL[] { url },
                getClass().getClassLoader()
        );
        try {
            SimiApiClass apiClass = (SimiApiClass) Class.forName(API_CLASS, true, loader).newInstance();
            for (String className : apiClass.classNames()) {
                classes.put(className, apiClass);
            }
            for (String globalMethodName : apiClass.globalMethodNames()) {
                globals.put(globalMethodName, apiClass);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
//        for (Class<?> clazz : javaApi.getClasses()) {
//            String name = clazz.getSimpleName();
//            if (name.equals(Constants.GLOBALS_CLASS_NAME)) {
//                for (Method method : clazz.getDeclaredMethods()) {
//                    globals.put(method.getName(), method);
//                }
//            } else {
//                try {
//                    classes.put(name, (SimiApiClass) clazz.newInstance());
//                } catch (InstantiationException | IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    SimiProperty call(String className,
                   String methodName,
                   SimiObject self,
                   Interpreter interpreter,
                   List<SimiProperty> args) throws IllegalArgumentException {
        if (className.equals(Constants.GLOBALS_CLASS_NAME)) {
            SimiApiClass apiClass = globals.get(methodName);
            if (apiClass != null) {
                return apiClass.call(className, methodName, self, interpreter, args);
            }
        } else {
            SimiApiClass apiClass = classes.get(className);
            if (apiClass != null) {
                return apiClass.call(className, methodName, self, interpreter, args);
            }
        }
        throw new IllegalArgumentException();
    }
}
