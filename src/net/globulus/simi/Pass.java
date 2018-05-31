package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

class Pass extends SimiValue {

    @Override
    public SimiValue copy() {
        return null;
    }

    @Override
    public SimiValue clone(boolean mutable) {
        return null;
    }

    @Override
    public int compareTo(SimiValue o) {
        throw new AssertionError();
    }

    @Override
    public java.lang.String toCode() {
        return TokenType.PASS.toCode();
    }
}
