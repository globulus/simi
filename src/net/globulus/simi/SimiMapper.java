package net.globulus.simi;

import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiProperty;
import net.globulus.simi.api.SimiValue;

import java.util.*;

public class SimiMapper {

    private SimiMapper() { }

    public static SimiObject toObject(Map<String, Object> map, boolean immutable) {
        LinkedHashMap<String, SimiProperty> propMap = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            propMap.put(entry.getKey(), toSimiProperty(entry.getValue()));
        }
        return new SimiObjectImpl.Dictionary(ActiveSimi.getObjectClass(), immutable, propMap);
    }

    public static SimiObject toObject(List<Object> list, boolean immutable) {
        ArrayList<SimiProperty> propList = new ArrayList<>(list.size());
        for (Object item : list) {
            propList.add(toSimiProperty(item));
        }
        return new SimiObjectImpl.Array(ActiveSimi.getObjectClass(), immutable, propList);

    }

    public static Map<String, SimiProperty> fromObject(SimiObject object) {
        if (((SimiObjectImpl) object).isArray()) {
            throw new IllegalArgumentException("Expected a dictionary object!");
        }
        return new HashMap<>(((SimiObjectImpl.Dictionary) object).fields);
    }

    public static SimiProperty toSimiProperty(Object value) {
        if (value instanceof Integer) {
            return new SimiValue.Number(((Integer) value).doubleValue());
        } else if (value instanceof Long) {
            return new SimiValue.Number(((Long) value).doubleValue());
        } else if (value instanceof Float) {
            return new SimiValue.Number(((Float) value).doubleValue());
        } else if (value instanceof Double) {
            return new SimiValue.Number((Double) value);
        } else if (value instanceof Boolean) {
            return new SimiValue.Number((Boolean) value ? 1 : 0);
        } else if (value instanceof String) {
            return new SimiValue.String((String) value);
        } else if (value instanceof Map) {
            return new SimiValue.Object(toObject((Map<String, Object>) value, true));
        } else if (value instanceof List) {
            return new SimiValue.Object(toObject((List<Object>) value, true));
        } else if (value instanceof SimiProperty) {
            return (SimiProperty) value;
        } else {
            throw new IllegalArgumentException("Unable to cast " + value.toString() + " to a SimiProperty!");
        }
    }
}
