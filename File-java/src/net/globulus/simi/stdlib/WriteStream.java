package net.globulus.simi.stdlib;

import net.globulus.simi.api.*;

import java.io.*;
import java.util.LinkedHashMap;

@SimiJavaClass
public class WriteStream {

    @SimiJavaMethod
    public static SimiValue init(SimiObject self, BlockInterpreter interpreter, SimiValue file) {
        SimiClass clazz = (SimiClass) self;
        String path = file.getObject().get("path", interpreter.getEnvironment()).getString();
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            Wrapper wrapper = new Wrapper(writer);
            LinkedHashMap<String, SimiValue> props = new LinkedHashMap<>();
            props.put("native_writer", new SimiValue.Object(wrapper));
            SimiObject object = interpreter.newInstance(clazz, props);
            return new SimiValue.Object(object);
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiValue write(SimiObject self, BlockInterpreter interpreter, SimiValue string) {
        try {
            getWriter(self, interpreter).write(string.getString());
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
        }
        return null;
    }

    @SimiJavaMethod
    public static SimiValue close(SimiObject self, BlockInterpreter interpreter) {
        try {
            getWriter(self, interpreter).close();
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
        }
        return null;
    }

    private static BufferedWriter getWriter(SimiObject self, BlockInterpreter interpreter) {
        return ((Wrapper) self.get("native_writer", interpreter.getEnvironment()).getObject()).writer;
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
    }
}
