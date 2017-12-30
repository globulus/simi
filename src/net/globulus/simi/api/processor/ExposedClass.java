package net.globulus.simi.api.processor;

import java.util.List;

/**
 * Created by gordanglavas on 20/11/2017.
 */

public class ExposedClass {

    public final String name;
    public final List<ExposedMethod> methods;

    public ExposedClass(String name, List<ExposedMethod> methods) {
        this.name = name;
        this.methods = methods;
    }
}
