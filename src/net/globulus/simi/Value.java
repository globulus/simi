package net.globulus.simi;

public abstract class Value<S extends Expr, T extends Value> {

    protected final S declaration;

    protected Value(S declaration) {
        this.declaration = declaration;
    }

    abstract T bind(SimiObjectImpl instance);
}
