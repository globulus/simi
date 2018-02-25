package net.globulus.simi;

import net.globulus.simi.api.SimiProperty;

import java.util.LinkedHashMap;

class SimiNativeObject extends SimiObjectImpl.Dictionary {

    SimiNativeObject(LinkedHashMap<String, SimiProperty> props) {
        super(null, true, props);
    }
}
