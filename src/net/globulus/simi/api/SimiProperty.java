package net.globulus.simi.api;

import java.util.List;

public interface SimiProperty {
    SimiValue getValue();
    void setValue(SimiValue value);
    List<SimiObject> getAnnotations();
    SimiProperty clone(boolean mutable);
}
