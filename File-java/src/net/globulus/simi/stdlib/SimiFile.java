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
    public static SimiValue readLines(SimiObject self, BlockInterpreter interpreter, SimiValue path) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(path.getString()));
            ArrayList<SimiValue> props = lines.stream()
                    .map(SimiValue.String::new)
                    .collect(Collectors.toCollection(ArrayList::new));
            return new SimiValue.Object(interpreter.newArray(true, props));
        } catch (IOException e) {
            e.printStackTrace();
            interpreter.raiseException(new SimiException((SimiClass) interpreter.getEnvironment().tryGet("IoException").getObject(), e.getMessage()));
            return null;
        }
    }
}
