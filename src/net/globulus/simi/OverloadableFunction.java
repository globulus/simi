package net.globulus.simi;

class OverloadableFunction {

    public final String name;
    public final int arity;

    OverloadableFunction(String name, int arity) {
        this.name = name;
        this.arity = arity;
    }

    @Override
    public int hashCode() {
        return (name + arity).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OverloadableFunction)) {
            return false;
        }
        OverloadableFunction of = (OverloadableFunction) obj;
        return name.equals(of.name) && arity == of.arity;
    }
}
