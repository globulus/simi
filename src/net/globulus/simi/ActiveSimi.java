package net.globulus.simi;

import net.globulus.simi.api.SimiProperty;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActiveSimi {

    private static Interpreter interpreter;
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    private static ImportResolver importResolver;

    private ActiveSimi() { }

    public static void load(String... files) throws IOException {
        ErrorHub.sharedInstance().removeWatcher(WATCHER);
        ErrorHub.sharedInstance().addWatcher(WATCHER);
        StringBuilder source = new StringBuilder("import \"stdlib/Stdlib.simi\"\n\n");
        for (String file : files) {
            source.append("import \"")
                    .append(file)
                    .append("\"\n");
        }
        run(source.toString());
    }

    public static SimiProperty eval(String className, String methodName, SimiProperty... params) {
        if (interpreter == null) {
            throw new IllegalStateException("Must call load() before using eval!");
        }
        StringBuilder sb = new StringBuilder();
        if (className != null) {
            sb.append(className).append('.');
        }
        sb.append(methodName).append('(');
        List<String> names = interpreter.defineTempVars(params);
        boolean first = true;
        for (String name : names) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(name);
        }
        sb.append(')');
        SimiProperty result = runExpression(sb.toString());
        interpreter.undefineTempVars(names);
        return result;
    }

    public static void evalAsync(Callback callback, String className, String methodName, SimiProperty... params) {
        synchronized (interpreter) {
            new Thread(() -> callback.done(eval(className, methodName, params))).run();
        }
    }

    public static void setImportResolver(ImportResolver ir) {
        importResolver = ir;
    }

    private static String readFile(String path) {
        if (importResolver == null) {
            throw new IllegalStateException("ImportResolver not initialized!");
        }
        return importResolver.readFile(path);
    }

    private static void run(String source) throws IOException {
        Map<String, NativeModulesManager> nativeModulesManagers = new HashMap<>();
        nativeModulesManagers.put("jar", new JavaNativeModulesManager());
        nativeModulesManagers.put("framework", new CocoaNativeModulesManager());
        List<String> imports = new ArrayList<>();

        long time = System.currentTimeMillis();
        System.out.print("Scanning and resolving imports...");
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanImports(scanner.scanTokens(true), imports, nativeModulesManagers);
        System.out.println(" " + (System.currentTimeMillis() - time) + " ms");
        time = System.currentTimeMillis();
        System.out.print("Parsing...");
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        interpreter = new Interpreter(nativeModulesManagers.values());

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        System.out.println(" " + (System.currentTimeMillis() - time) + " ms");
        time = System.currentTimeMillis();
        // Stop if there was a resolution error.
        if (hadError) return;

        interpreter.interpret(statements);
        System.out.println("Interpreting... " + (System.currentTimeMillis() - time) + " ms");
    }

    private static SimiProperty runExpression(String expression) {
        List<Token> tokens  = new Scanner(expression).scanTokens(true);
        List<Stmt> statements = new Parser(tokens).parse();
        return interpreter.interpret(statements);
    }

    private static List<Token> scanImports(List<Token> input,
                                           List<String> imports,
                                           Map<String, NativeModulesManager> nativeModulesManagers) throws IOException {
        List<Token> result = new ArrayList<>();
        int len = input.size();
        for (int i = 0; i < len; i++) {
            Token token = input.get(i);
            if (token.type != TokenType.IMPORT) {
                continue;
            }
            i++;
            Token nextToken = input.get(i);
            if (nextToken.type != TokenType.STRING) {
                continue;
            }
            String location = nextToken.literal.getString();
            if (imports.contains(location)) {
                continue;
            }
            Path path = Paths.get(location);
            String pathString = path.toString().toLowerCase();
            if (pathString.endsWith(".simi")) {
                List<Token> tokens = new Scanner(readFile(location)).scanTokens(false);
                result.addAll(scanImports(tokens, imports, nativeModulesManagers));
            } else {
                String extension = pathString.substring(pathString.lastIndexOf('.'));
                NativeModulesManager manager = nativeModulesManagers.get(extension);
                if (manager != null) {
                    manager.load(path.toUri().toURL().toString());
                }
            }
        }
        result.addAll(input);
        return result;
    }

    private static final ErrorWatcher WATCHER = new ErrorWatcher() {
        @Override
        public void report(int line, String where, String message) {
            System.err.println(
                    "[line " + line + "] Error" + where + ": " + message);
            hadError = true;
        }

        @Override
        public void runtimeError(RuntimeError error) {

            System.err.println(error.getMessage() +
                    "\n[line " + error.token.line + "]");
            hadRuntimeError = true;
        }
    };

    static SimiClassImpl getObjectClass() {
        if (interpreter == null) {
            return null;
        }
        return (SimiClassImpl) interpreter.getGlobal(Constants.CLASS_OBJECT).getValue().getObject();
    }

    public interface ImportResolver {
        String readFile(String fileName);
    }

    public interface Callback {
        void done(SimiProperty result);
    }
}
