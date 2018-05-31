package net.globulus.simi;

import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiProperty;

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
    public SimiProperty call(BlockInterpreter interpreter, List<SimiProperty> arguments, boolean rethrow) {
        return function.call(interpreter, arguments, rethrow);
    }

    @Override
    public String toCode() {
        return function.toCode();
    }
}
