package net.globulus.simi.stdlib;

import net.globulus.simi.api.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

@SimiJavaClass
public class ReadStream {

    @SimiJavaMethod
    public static SimiValue init(SimiObject self, BlockInterpreter interpreter, SimiValue file) {
        SimiClass clazz = (SimiClass) self;
        String path = file.getObject().get("path", interpreter.getEnvironment()).value.getString();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            Wrapper wrapper = new Wrapper(reader);
            LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
            props.put("native_reader", new SimiProperty(new SimiValue.Object(wrapper)));
            SimiObject object = interpreter.newInstance(clazz, props);
            return new SimiValue.Object(object);
        } catch (FileNotFoundException e) {
            Utils.raiseIoException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiValue read(SimiObject self, BlockInterpreter interpreter) {
        try {
            return new SimiValue.String("" + (char) getReader(self, interpreter).read());
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
            return new SimiValue.String("");
        }
    }

    @SimiJavaMethod
    public static SimiValue readLine(SimiObject self, BlockInterpreter interpreter) {
        try {
            String line = getReader(self, interpreter).readLine();
            if (line != null) {
                return new SimiValue.String(line);
            } else {
                return null;
            }
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
            return new SimiValue.String("");
        }
    }

    @SimiJavaMethod
    public static SimiValue reset(SimiObject self, BlockInterpreter interpreter) {
        try {
            getReader(self, interpreter).reset();
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
        }
        return null;
    }

    @SimiJavaMethod
    public static SimiValue skip(SimiObject self, BlockInterpreter interpreter, SimiValue length) {
        try {
            return new SimiValue.Number(getReader(self, interpreter).skip(length.getNumber().longValue()));
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiValue close(SimiObject self, BlockInterpreter interpreter) {
        try {
            getReader(self, interpreter).close();
        } catch (IOException e) {
            Utils.raiseIoException(e, interpreter);
        }
        return null;
    }

    private static BufferedReader getReader(SimiObject self, BlockInterpreter interpreter) {
        return ((Wrapper) self.get("native_reader", interpreter.getEnvironment()).value.getObject()).reader;
    }

    private static class Wrapper implements SimiObject {

        BufferedReader reader;

        Wrapper(BufferedReader reader) {
            this.reader = reader;
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
