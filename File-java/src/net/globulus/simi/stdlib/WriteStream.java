package net.globulus.simi.stdlib;

import net.globulus.simi.api.*;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;

@SimiJavaClass
public class WriteStream {

    @SimiJavaMethod
    public static SimiProperty init(SimiObject self, BlockInterpreter interpreter, SimiProperty file) {
        SimiClass clazz = (SimiClass) self;
        String path = file.getValue().getObject().get("path", interpreter.getEnvironment()).getValue().getString();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            Wrapper wrapper = new Wrapper(writer);
            LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
            props.put("native_writer", new SimiValue.Object(wrapper));
            SimiObject object = interpreter.newInstance(clazz, props);
            return new SimiValue.Object(object);
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty write(SimiObject self, BlockInterpreter interpreter, SimiProperty string) {
        try {
            getWriter(self, interpreter).write(string.getValue().getString());
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
        }
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty close(SimiObject self, BlockInterpreter interpreter) {
        try {
            getWriter(self, interpreter).close();
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
        }
        return null;
    }

    private static BufferedWriter getWriter(SimiObject self, BlockInterpreter interpreter) {
        return ((Wrapper) self.get("native_writer", interpreter.getEnvironment()).getValue().getObject()).writer;
    }

    private static class Wrapper implements SimiObject {

        BufferedWriter writer;

        Wrapper(BufferedWriter writer) {
            this.writer = writer;
        }

        @Override
        public SimiClass getSimiClass() {
            return null;
        }

        @Override
        public SimiProperty get(String s, SimiEnvironment simiEnvironment) {
            return null;
        }

        @Override
        public void set(String s, SimiProperty simiProperty, SimiEnvironment simiEnvironment) {

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
