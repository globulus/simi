package net.globulus.simi.stdlib;

import net.globulus.simi.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SimiJavaClass(name = "File")
public class SimiFile {

    @SimiJavaMethod
    public static SimiProperty readLines(SimiObject self, BlockInterpreter interpreter, SimiProperty path) {
        ArrayList<SimiValue> props;
        try {
            List<String> lines = Files.readAllLines(Paths.get(path.getValue().getString()));
            props = lines.stream()
                    .map(SimiValue.String::new)
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (IOException e) {
//            e.printStackTrace();
            Utils.raiseIoException(e, interpreter);
            props = new ArrayList<>();
        }
        return new SimiValue.Object(interpreter.newArray(true, props));
    }

    @SimiJavaMethod
    public static SimiProperty readString(SimiObject self, BlockInterpreter interpreter, SimiProperty path) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(path.getValue().getString())));
            return new SimiValue.String(content);
        } catch (IOException e) {
//            e.printStackTrace();
            Utils.raiseIoException(e, interpreter);
            return null;
        }
    }
}
