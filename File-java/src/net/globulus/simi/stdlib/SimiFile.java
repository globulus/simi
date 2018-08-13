package net.globulus.simi.stdlib;

import net.globulus.simi.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SimiJavaConfig(apiClassName = "File_java")
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

    @SimiJavaMethod
    public static SimiProperty listAll(SimiObject self,
                                       BlockInterpreter interpreter,
                                       SimiProperty path,
                                       SimiProperty filter,
                                       SimiProperty recursive) {
        int maxDepth = (recursive.getValue().getNumber() == 0) ? 1 : 999;
        String filterRegex;
        if (filter.getValue() instanceof SimiValue.String) {
            filterRegex = filter.getValue().getString();
        } else {
            filterRegex = "(" + filter.getValue().getObject().values().stream()
                    .map(SimiValue::getString)
                    .collect(Collectors.joining("|")) + ")";
        }
        try {
            return new SimiValue.Object(interpreter.newArray(true,
                    Files.find(Paths.get(path.getValue().getString()), maxDepth, (p, bfa) ->
                        p.getFileName().toString().matches(".*\\." + filterRegex + "$")
                    )
                            .map(p -> pathToSimiFile((SimiClass) self, interpreter, p))
                            .collect(Collectors.toCollection(ArrayList::new))
            ));
        } catch (IOException e) {
//            e.printStackTrace();
            Utils.raiseIoException(e, interpreter);
            return null;
        }
    }

    private static SimiValue pathToSimiFile(SimiClass clazz, BlockInterpreter interpreter, Path path) {
        LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
        props.put("path", new SimiValue.String(path.toString()));
        return new SimiValue.Object(interpreter.newInstance(clazz, props));
    }
}
