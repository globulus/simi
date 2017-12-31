package net.globulus.simi.api;

public class SimiValue {

    private SimiValue() { }

    public static class String {

        public final java.lang.String value;

        public String(java.lang.String value) {
            this.value = value;
        }
    }

    public static class Number {

        public final double value;

        public Number(double value) {
            this.value = value;
        }
    }

    public static class Object {

        public final SimiObject value;

        public Object(SimiObject value) {
            this.value = value;
        }
    }
}
