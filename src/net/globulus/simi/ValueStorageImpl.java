package net.globulus.simi;

import net.globulus.simi.api.SimiCallable;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

class ValueStorageImpl implements ValueStorage {

    private final Map<String, Double> numbers = new HashMap<>();
    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, SimiObject> objects = new HashMap<>();
    private final Map<String, SimiCallable> callables = new HashMap<>();

    private final Map<String, Map<String, ?>> values = new LinkedHashMap<>();


    @Override
    public void put(String name, Double value) {
        numbers.put(name, value);
        values.put(name, numbers);
    }

    @Override
    public void put(String name, String value) {
        strings.put(name, value);
        values.put(name, strings);
    }

    @Override
    public void put(String name, SimiObject value) {
        objects.put(name, value);
        values.put(name, objects);
    }

    @Override
    public void put(String name, SimiCallable value) {
        callables.put(name, value);
        values.put(name, callables);
    }

    @Override
    public void put(String name, SimiValue value) {
        if (value instanceof SimiValue.Number) {
            put(name, value.getNumber());
        } else if (value instanceof SimiValue.String) {
            put(name, value.getString());
        } else if (value instanceof SimiValue.Object) {
            put(name, value.getObject());
        } else if (value instanceof SimiValue.Callable) {
            put(name, value.getCallable());
        }
    }

    @Override
    public void put(String name, Object value) {
        if (value instanceof Double) {
            put(name, (Double) value);
        } else if (value instanceof String) {
            put(name, (String) value);
        } else if (value instanceof SimiObject) {
            put(name, (SimiObject) value);
        } else if (value instanceof SimiCallable) {
            put(name, (SimiCallable) value);
        }
    }

    @Override
    public Double getNumber(String name) {
        return numbers.get(name);
    }

    @Override
    public String getString(String name) {
        return strings.get(name);
    }

    @Override
    public SimiObject getObject(String name) {
        return objects.get(name);
    }

    @Override
    public SimiCallable getCallable(String name) {
        return callables.get(name);
    }

    @Override
    public SimiValue getValue(String name) {
        Map<String, ?> map = values.get(name);
        if (map == numbers) {
            return new SimiValue.Number(numbers.get(name));
        }
        if (map == strings) {
            return new SimiValue.String(strings.get(name));
        }
        if (map == objects) {
            return new SimiValue.Object(objects.get(name));
        }
        if (map == callables) {
            return new SimiValue.Callable(callables.get(name), name, null);
        }
        return null;
    }

    @Override
    public Object get(String name) {
        Map<String, ?> map = values.get(name);
        if (map != null) {
            return map.get(name);
        }
        return null;
    }

    @Override
    public boolean remove(String name) {
        Map<String, ?> map = values.get(name);
        if (map != null) {
            map.remove(name);
            values.remove(name);
            return true;
        }
        return false;
    }
}
