package net.globulus.simi;

import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiValue;

import java.util.List;

class SimiMethod implements SimiCallable {

    final SimiClassImpl clazz;
    final SimiFunction function;

    SimiMethod(SimiClassImpl clazz, SimiFunction function) {
        this.clazz = clazz;
        this.function = function;
    }

    @Override
    public int arity() {
        return function.arity();
    }

    @Override
    public SimiValue call(BlockInterpreter interpreter, List<SimiValue> arguments, boolean rethrow) {
        return function.call(interpreter, arguments, rethrow);
    }
}
