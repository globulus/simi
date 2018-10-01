package net.globulus.simi.api;

import net.globulus.simi.api.SimiProperty;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.BlockInterpreter;

/**
 * Generated class by @JavaApi . Do not modify this code!
 */
public class JavaApi
    implements SimiApiClass {

  public JavaApi() {
  }

  public SimiProperty call(String className, String methodName, SimiObject self, BlockInterpreter interpreter, java.util.List<SimiProperty> args) {
    switch (className) {
      case "ResultSet":
      switch (methodName) {
        case "moveTo": return ResultSet.moveTo(self, interpreter, args.get(0));
        case "moveBy": return ResultSet.moveBy(self, interpreter, args.get(0));
        case "first": return ResultSet.first(self, interpreter);
        case "last": return ResultSet.last(self, interpreter);
        case "isFirst": return ResultSet.isFirst(self, interpreter);
        case "isLast": return ResultSet.isLast(self, interpreter);
        case "isClosed": return ResultSet.isClosed(self, interpreter);
        case "previous": return ResultSet.previous(self, interpreter);
        case "afterLast": return ResultSet.afterLast(self, interpreter);
        case "beforeFirst": return ResultSet.beforeFirst(self, interpreter);
        case "next": return ResultSet.next(self, interpreter);
        case "close": return ResultSet.close(self, interpreter);
        case "findColumn": return ResultSet.findColumn(self, interpreter, args.get(0));
        case "getBoolean": return ResultSet.getBoolean(self, interpreter, args.get(0));
        case "getInt": return ResultSet.getInt(self, interpreter, args.get(0));
        case "getLong": return ResultSet.getLong(self, interpreter, args.get(0));
        case "getFloat": return ResultSet.getFloat(self, interpreter, args.get(0));
        case "getDouble": return ResultSet.getDouble(self, interpreter, args.get(0));
        case "getString": return ResultSet.getString(self, interpreter, args.get(0));
        case "getDate": return ResultSet.getDate(self, interpreter, args.get(0));
        case "getTime": return ResultSet.getTime(self, interpreter, args.get(0));
        default: return null;
      }
      case "MariaDb":
      switch (methodName) {
        case "open": return MariaDb.open(self, interpreter, args.get(0));
        case "close": return MariaDb.close(self, interpreter);
        case "execute": return MariaDb.execute(self, interpreter, args.get(0));
        default: return null;
      }
      default: return null;
    }
  }

  public String[] classNames() {
    return new String[] { "ResultSet","MariaDb" };
  }

  public String[] globalMethodNames() {
    return new String[] {  };
  }


  public static class ResultSet {

    private static SimiProperty moveTo(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.moveTo(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty moveBy(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.moveBy(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty first(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.first(simiobject0, blockinterpreter1);
    }

    private static SimiProperty last(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.last(simiobject0, blockinterpreter1);
    }

    private static SimiProperty isFirst(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.isFirst(simiobject0, blockinterpreter1);
    }

    private static SimiProperty isLast(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.isLast(simiobject0, blockinterpreter1);
    }

    private static SimiProperty isClosed(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.isClosed(simiobject0, blockinterpreter1);
    }

    private static SimiProperty previous(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.previous(simiobject0, blockinterpreter1);
    }

    private static SimiProperty afterLast(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.afterLast(simiobject0, blockinterpreter1);
    }

    private static SimiProperty beforeFirst(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.beforeFirst(simiobject0, blockinterpreter1);
    }

    private static SimiProperty next(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.next(simiobject0, blockinterpreter1);
    }

    private static SimiProperty close(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.SimiResultSet.close(simiobject0, blockinterpreter1);
    }

    private static SimiProperty findColumn(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.findColumn(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty getBoolean(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.getBoolean(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty getInt(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.getInt(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty getLong(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.getLong(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty getFloat(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.getFloat(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty getDouble(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.getDouble(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty getString(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.getString(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty getDate(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.getDate(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty getTime(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.SimiResultSet.getTime(simiobject0, blockinterpreter1, simiproperty2);
    }
  }


  public static class MariaDb {

    private static SimiProperty open(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.MariaDb.open(simiobject0, blockinterpreter1, simiproperty2);
    }

    private static SimiProperty close(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.sql.MariaDb.close(simiobject0, blockinterpreter1);
    }

    private static SimiProperty execute(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.sql.MariaDb.execute(simiobject0, blockinterpreter1, simiproperty2);
    }
  }

}
