package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

class TempNull extends SimiValue {

    static final TempNull INSTANCE = new TempNull();

    private TempNull() { }

    @Override
    public SimiValue copy() {
        return this;
    }

    @Override
    public SimiValue clone(boolean mutable) {
        return this;
    }

    @Override
    public int compareTo(SimiValue o) {
        return 0;
    }
}
