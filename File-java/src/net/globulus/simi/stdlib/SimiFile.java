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
        ArrayList<SimiValue> props;
        try {
            List<String> lines = Files.readAllLines(Paths.get(path.getString()));
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
}
