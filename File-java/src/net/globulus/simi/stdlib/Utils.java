package net.globulus.simi.stdlib;

import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiClass;
import net.globulus.simi.api.SimiException;

import java.io.IOException;

class Utils {

    static void raiseIoException(IOException e, BlockInterpreter interpreter) {
        interpreter.raiseException(new SimiException((SimiClass) interpreter.getEnvironment().tryGet("IoException").getObject(), e.getMessage()));
    }
}
