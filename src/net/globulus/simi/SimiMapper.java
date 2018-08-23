package net.globulus.simi;

import net.globulus.simi.api.BlockInterpreter;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiProperty;
import net.globulus.simi.api.SimiValue;

import java.util.*;
import java.util.stream.Collectors;

public class SimiMapper {

    private SimiMapper() { }

    public static SimiObject toObject(Map<String, Object> map, boolean immutable) {
       return toObject(map, immutable, ActiveSimi.getObjectClass());
    }

    public static SimiObject toObject(Map<String, Object> map, boolean immutable, BlockInterpreter interpreter) {
        return toObject(map, immutable, getObjectClass(interpreter));
    }

    private static SimiObject toObject(Map<String, Object> map, boolean immutable, SimiClassImpl objectClass) {
        LinkedHashMap<String, SimiProperty> propMap = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            propMap.put(entry.getKey(), toSimiProperty(entry.getValue(), objectClass));
        }
        return new SimiObjectImpl.Dictionary(objectClass, immutable, propMap);
    }

    public static SimiObject toObject(List<Object> list, boolean immutable) {
        return toObject(list, immutable, ActiveSimi.getObjectClass());
    }

    public static SimiObject toObject(List<Object> list, boolean immutable, BlockInterpreter interpreter) {
        return toObject(list, immutable, getObjectClass(interpreter));
    }

    private static SimiObject toObject(List<Object> list, boolean immutable, SimiClassImpl objectClass) {
        ArrayList<SimiProperty> propList = new ArrayList<>(list.size());
        for (Object item : list) {
            propList.add(toSimiProperty(item, objectClass));
        }
        return new SimiObjectImpl.Array(objectClass, immutable, propList);
    }

    public static Map<String, Object> fromObject(SimiObject object) {
        if (((SimiObjectImpl) object).isArray()) {
            throw new IllegalArgumentException("Expected a dictionary object!");
        }
        SimiObjectImpl.Dictionary dict;
        if (object instanceof SimiObjectImpl.Dictionary) {
            dict = (SimiObjectImpl.Dictionary) object;
        } else {
            dict = ((SimiObjectImpl.InitiallyEmpty) object).asDictionary();
        }
        Map<String, Object> map = new HashMap<>(dict.fields.size());
        for (Map.Entry<String, SimiProperty> entry : dict.fields.entrySet()) {
            map.put(entry.getKey(), fromSimiValue(entry.getValue().getValue()));
        }
        return map;
    }

    public static List<Object> fromArray(SimiObject object) {
        if (!((SimiObjectImpl) object).isArray()) {
            throw new IllegalArgumentException("Expected an array object!");
        }
        SimiObjectImpl.Array array;
        if (object instanceof SimiObjectImpl.Array) {
            array = (SimiObjectImpl.Array) object;
        } else {
            array = ((SimiObjectImpl.InitiallyEmpty) object).asArray();
        }
        return array.fields.stream()
                .map(SimiProperty::getValue)
                .map(SimiMapper::fromSimiValue)
                .collect(Collectors.toList());
    }

    public static SimiProperty toSimiProperty(Object value) {
        return toSimiProperty(value, ActiveSimi.getObjectClass());
    }

    public static SimiProperty toSimiProperty(Object value, BlockInterpreter interpreter) {
        return toSimiProperty(value, getObjectClass(interpreter));
    }

    private static SimiProperty toSimiProperty(Object value, SimiClassImpl objectClass) {
        if (value instanceof Integer) {
            return new SimiValue.Number(((Integer) value).longValue());
        } else if (value instanceof Long) {
            return new SimiValue.Number(((Long) value));
        } else if (value instanceof Float) {
            return new SimiValue.Number(((Float) value).doubleValue());
        } else if (value instanceof Double) {
            double num = (Double) value;
            if (num == Math.floor(num)) {
                return new SimiValue.Number(Math.round(num));
            }
            return new SimiValue.Number(num);
        } else if (value instanceof Boolean) {
            return new SimiValue.Number((Boolean) value ? 1 : 0);
        } else if (value instanceof String) {
            return new SimiValue.String((String) value);
        } else if (value instanceof Map) {
            return new SimiValue.Object(toObject((Map<String, Object>) value, true, objectClass));
        } else if (value instanceof List) {
            return new SimiValue.Object(toObject((List<Object>) value, true, objectClass));
        } else if (value instanceof SimiProperty) {
            return (SimiProperty) value;
        } else {
            throw new IllegalArgumentException("Unable to cast " + value.toString() + " to a SimiProperty!");
        }
    }

    public static Object fromSimiValue(SimiValue value) {
        if (value instanceof SimiValue.Number) {
            return value.getNumber().getJavaValue();
        } else if (value instanceof SimiValue.String) {
            return value.getString();
        } else if (value instanceof SimiValue.Object) {
            SimiObjectImpl obj = (SimiObjectImpl) value.getObject();
            if (obj.isArray()) {
                return fromArray(obj);
            } else {
                return fromObject(obj);
            }
        } else {
            return value;
        }
    }

    private static SimiClassImpl getObjectClass(BlockInterpreter interpreter) {
        return (SimiClassImpl) interpreter.getGlobal(Constants.CLASS_OBJECT).getValue().getObject();
    }
}
