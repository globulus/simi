package net.globulus.simi;

import net.globulus.simi.api.SimiClass;
import net.globulus.simi.api.SimiEnvironment;
import net.globulus.simi.api.SimiValue;

import java.util.LinkedHashMap;

class SimiNativeObject extends SimiObjectImpl {

    SimiNativeObject(LinkedHashMap<String, SimiValue> fields) {
        super(null, fields, true);
    }
}
