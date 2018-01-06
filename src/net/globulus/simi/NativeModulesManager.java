package net.globulus.simi;

import net.globulus.simi.api.Constants;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

class NativeModulesManager {

    private static final String API_CLASS = Constants.PACKAGE_SIMI_API + "." + Constants.API_CLASS_NAME;

    private Map<String, Method> globals;
    private Map<String, Class<?>> classes;

    public NativeModulesManager() {
        globals = new HashMap<>();
        classes = new HashMap<>();
    }

    public void loadJar(URL url) {
        ClassLoader loader = URLClassLoader.newInstance(
                new URL[] { url },
                getClass().getClassLoader()
        );
        Class<?> javaApi;
        try {
            javaApi = Class.forName(API_CLASS, true, loader);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }
        for (Class<?> clazz : javaApi.getClasses()) {
            String name = clazz.getSimpleName();
            if (name.equals(Constants.GLOBALS_CLASS_NAME)) {
                for (Method method : clazz.getDeclaredMethods()) {
                    globals.put(method.getName(), method);
                }
            } else {
                classes.put(name, clazz);
            }
        }
    }
}
