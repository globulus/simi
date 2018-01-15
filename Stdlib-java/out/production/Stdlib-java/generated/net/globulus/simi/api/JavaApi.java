package net.globulus.simi.api;

import net.globulus.simi.api.SimiValue;
import net.globulus.simi.api.SimiObject;
import net.globulus.simi.api.SimiEnvironment;

/**
 * Generated class by @JavaApi . Do not modify this code!
 */
public class JavaApi
    implements SimiApiClass {

  public JavaApi() {
  }

  public SimiValue call(String className, String methodName, SimiObject self, SimiEnvironment environment, java.util.List<SimiValue> args) {
    switch (className) {
      case "Date":
      switch (methodName) {
        case "format": return Date.format(self, environment, args.get(0));
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

    private static SimiValue format(SimiObject simiobject0, SimiEnvironment simienvironment1, SimiValue simivalue2) {
      return net.globulus.simi.testing.SimiDate.format(simiobject0, simienvironment1, simivalue2);
    }
  }

}
