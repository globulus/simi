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
      case "Date":
      switch (methodName) {
        case "now": return Date.now(self, interpreter);
        case "format": return Date.format(self, interpreter, args.get(0));
        default: return null;
      }
      default: return null;
    }
  }

  public String[] classNames() {
    return new String[] { "Date" };
  }

  public String[] globalMethodNames() {
    return new String[] {  };
  }


  public static class Date {

    private static SimiProperty now(SimiObject simiobject0, BlockInterpreter blockinterpreter1) {
      return net.globulus.simi.stdlib.SimiDate.now(simiobject0, blockinterpreter1);
    }

    private static SimiProperty format(SimiObject simiobject0, BlockInterpreter blockinterpreter1, SimiProperty simiproperty2) {
      return net.globulus.simi.stdlib.SimiDate.format(simiobject0, blockinterpreter1, simiproperty2);
    }
  }

}
