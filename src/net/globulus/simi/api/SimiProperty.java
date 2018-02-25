package net.globulus.simi.api;

import java.util.List;

public final class SimiProperty implements Comparable<SimiProperty> {

    public SimiValue value;
    public final List<SimiObject> annotations;

    public SimiProperty(SimiValue value) {
        this.value = value;
        this.annotations = null;
    }

    public SimiProperty(SimiValue value, List<SimiObject> annotations) {
        this.value = value;
        this.annotations = annotations;
    }

    public SimiProperty clone(boolean mutable) {
        return new SimiProperty(value.clone(mutable), annotations);
    }

    @Override
    public int compareTo(SimiProperty o) {
        return value.compareTo(o.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
