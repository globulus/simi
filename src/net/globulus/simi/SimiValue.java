package net.globulus.simi;

public abstract class SimiValue<S extends Expr, T extends SimiValue> {

    protected final S declaration;

    protected SimiValue(S declaration) {
        this.declaration = declaration;
    }

    abstract T bind(SimiObjectImpl instance);
}
