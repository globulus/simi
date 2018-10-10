package net.globulus.simi.sql;

import net.globulus.simi.api.*;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.LinkedHashMap;
import java.util.List;

@SimiJavaClass(name = SimiResultSet.NAME)
public class SimiResultSet {

    static final String NAME = "ResultSet";
    private static final String NATIVE_RESULT_SET = "native_result_set";

    static SimiProperty init(ResultSet resultSet, BlockInterpreter interpreter) {
        SimiClass clazz = (SimiClass) interpreter.getEnvironment().tryGet(NAME).getValue().getObject();
        LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
        props.put(NATIVE_RESULT_SET, new SimiValue.Object(new Wrapper(resultSet)));
        return new SimiValue.Object(interpreter.newInstance(clazz, props));
    }

    @SimiJavaMethod
    public static SimiProperty moveTo(SimiObject self, BlockInterpreter interpreter, SimiProperty row) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.absolute(Math.toIntExact(row.getValue().getNumber().asLong()));
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty moveBy(SimiObject self, BlockInterpreter interpreter, SimiProperty row) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.relative(Math.toIntExact(row.getValue().getNumber().asLong()));
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty first(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.first();
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty last(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.last();
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty isFirst(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.isFirst();
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty isLast(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.isLast();
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty isClosed(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.isClosed();
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty previous(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.previous();
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty afterLast(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            resultSet.afterLast();
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
        }
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty beforeFirst(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            resultSet.beforeFirst();
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
        }
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty next(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.next();
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            e.printStackTrace();
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty close(SimiObject self, BlockInterpreter interpreter) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            resultSet.close();
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
        }
        return null;
    }

    @SimiJavaMethod
    public static SimiProperty findColumn(SimiObject self, BlockInterpreter interpreter, SimiProperty label) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            int result = resultSet.findColumn(label.getValue().getString());
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty getBoolean(SimiObject self, BlockInterpreter interpreter, SimiProperty label) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            boolean result = resultSet.getBoolean(label.getValue().getString());
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty getInt(SimiObject self, BlockInterpreter interpreter, SimiProperty label) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            int result = resultSet.getInt(label.getValue().getString());
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty getLong(SimiObject self, BlockInterpreter interpreter, SimiProperty label) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            long result = resultSet.getLong(label.getValue().getString());
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty getFloat(SimiObject self, BlockInterpreter interpreter, SimiProperty label) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            float result = resultSet.getFloat(label.getValue().getString());
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty getDouble(SimiObject self, BlockInterpreter interpreter, SimiProperty label) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            double result = resultSet.getDouble(label.getValue().getString());
            return new SimiValue.Number(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty getString(SimiObject self, BlockInterpreter interpreter, SimiProperty label) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            String result = resultSet.getString(label.getValue().getString());
            return new SimiValue.String(result);
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty getDate(SimiObject self, BlockInterpreter interpreter, SimiProperty label) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            Date result = resultSet.getDate(label.getValue().getString());
            SimiClass clazz = (SimiClass) interpreter.getEnvironment().tryGet("Date").getValue().getObject();
            LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
            props.put("timestamp", new SimiValue.Number(result.getTime()));
            return new SimiValue.Object(interpreter.newInstance(clazz, props));
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    @SimiJavaMethod
    public static SimiProperty getTime(SimiObject self, BlockInterpreter interpreter, SimiProperty label) {
        ResultSet resultSet = getNative(self, interpreter);
        try {
            Time result = resultSet.getTime(label.getValue().getString());
            SimiClass clazz = (SimiClass) interpreter.getEnvironment().tryGet("Date").getValue().getObject();
            LinkedHashMap<String, SimiProperty> props = new LinkedHashMap<>();
            props.put("timestamp", new SimiValue.Number(result.getTime()));
            return new SimiValue.Object(interpreter.newInstance(clazz, props));
        } catch (SQLException e) {
            Util.raiseSqlException(e, interpreter);
            return null;
        }
    }

    private static ResultSet getNative(SimiObject self, BlockInterpreter interpreter) {
        return ((Wrapper) self.get(NATIVE_RESULT_SET, interpreter.getEnvironment()).getValue().getObject()).resultSet;
    }

    private static class Wrapper implements SimiObject {

        ResultSet resultSet;

        Wrapper(ResultSet resultSet) {
            this.resultSet = resultSet;
        }

        @Override
        public SimiClass getSimiClass() {
            return null;
        }

        @Override
        public SimiProperty get(String s, SimiEnvironment simiEnvironment) {
            return null;
        }

        @Override
        public void set(String s, SimiProperty simiProperty, SimiEnvironment simiEnvironment) {

        }

        @Override
        public SimiObject clone(boolean b) {
            return null;
        }

        @Override
        public List<SimiValue> keys() {
            return null;
        }

        @Override
        public List<SimiValue> values() {
            return null;
        }

        @Override
        public String toCode(int i, boolean b) {
            return null;
        }

        @Override
        public int getLineNumber() {
            return 0;
        }

        @Override
        public String getFileName() {
            return null;
        }

        @Override
        public boolean hasBreakPoint() {
            return false;
        }

        @Override
        public int compareTo(SimiObject o) {
            return 0;
        }
    }
}
