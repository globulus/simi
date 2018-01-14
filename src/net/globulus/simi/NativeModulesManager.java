package net.globulus.simi;

import net.globulus.simi.api.Constants;
import net.globulus.simi.api.SimiApiClass;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

class NativeModulesManager {

    private static final String API_CLASS = Constants.PACKAGE_SIMI_API + "." + Constants.API_CLASS_NAME;

    private Map<String, SimiApiClass> classes;

    public NativeModulesManager() {
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
            try {
                classes.put(API_CLASS, (SimiApiClass) javaApi.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
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
}
