package net.globulus.simi.api.processor.codegen;

import net.globulus.simi.api.processor.ExposedMethod;
import net.globulus.simi.api.processor.javawriter.SimiApiJavaWriter;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.EnumSet;

public class ExposedMethodCodeGen implements CodeGen<ExposedMethod> {

    @Override
    public void generateCode(ExposedMethod method, SimiApiJavaWriter jw) throws IOException {
        jw.emitEmptyLine();
        jw.beginMethod(method.returnType, method.name, EnumSet.of(Modifier.PRIVATE, Modifier.STATIC), method.params, method.thrown);
        StringBuilder params = new StringBuilder();
        for (int i = 1; i < method.params.size(); i += 2) {
            if (i > 1) {
                params.append(", ");
            }
            params.append(method.params.get(i));
        }
        String start;
        if (method.returnType.equals("void")) {
            start = "";
        } else {
            start = "return ";
        }
        jw.emitStatement("%s%s(%s)", start, method.originalMethod, params.toString());
        jw.endMethod();
    }
}
