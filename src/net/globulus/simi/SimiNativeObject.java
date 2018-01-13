package net.globulus.simi;

import net.globulus.simi.api.SimiValue;

import java.util.LinkedHashMap;

class SimiNativeObject extends SimiObjectImpl.Dictionary {

    SimiNativeObject(LinkedHashMap<String, SimiValue> fields) {
        super(null, true, fields);
    }
}
