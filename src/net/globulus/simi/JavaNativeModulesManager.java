package net.globulus.simi;

import net.globulus.simi.api.*;
import net.globulus.simi.api.Constants;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JavaNativeModulesManager implements NativeModulesManager {

    private static final String API_CLASS = Constants.PACKAGE_SIMI_API + "." + Constants.API_CLASS_NAME;

    private Map<String, SimiApiClass> classes;
    private Map<String, SimiApiClass> globals;

    public JavaNativeModulesManager() {
        classes = new HashMap<>();
        globals = new HashMap<>();
    }

    @Override
    public void load(String path, boolean useApiClassName) {
       load(path, useApiClassName, useApiClassName);
    }

    public void load(String path, boolean useApiClassName, boolean useCustomLoader) {
            try {
                SimiApiClass apiClass;
                ClassLoader loader;
                if (useCustomLoader) {
                    URL url = new URL(path);
                    loader = URLClassLoader.newInstance(new URL[]{url}, getClass().getClassLoader());
                    apiClass = (SimiApiClass) Class.forName(getApiClassName(path, useApiClassName), true, loader).newInstance();
                } else {
                    apiClass = (SimiApiClass) Class.forName(getApiClassName(path, useApiClassName)).newInstance();
                }

                for (String className : apiClass.classNames()) {
                    classes.put(className, apiClass);
                }
                for (String globalMethodName : apiClass.globalMethodNames()) {
                    globals.put(globalMethodName, apiClass);
                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                if (useApiClassName) {
                    load(path, false, true);
                } else if (useCustomLoader) {
                    load(path, false, false);
                } else {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
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

    private String getApiClassName(String path, boolean useApiClassName) {
        if (useApiClassName) {
            return API_CLASS;
        }
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        fileName = fileName.substring(0, fileName.indexOf('.'));
        return Constants.PACKAGE_SIMI_API + "." + fileName.replace('-', '_');
    }

    @Override
    public SimiProperty call(String className,
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
                Environment environment = (Environment) interpreter.getEnvironment();
                environment.assign(Token.self(), new SimiValue.Object(self), true);
                SimiClassImpl clazz = (SimiClassImpl) self.getSimiClass();
                if (clazz != null) {
                    environment.assign(Token.superToken(), new SimiClassImpl.SuperClassesList(clazz.superclasses), true);
                }
                return apiClass.call(className, methodName, self, interpreter, args);
            }
        }
        throw new IllegalArgumentException();
    }
}
