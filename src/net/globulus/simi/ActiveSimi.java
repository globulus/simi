package net.globulus.simi;

import net.globulus.simi.api.SimiProperty;

import java.net.URL;
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

    private static List<String> resolvedImports = new ArrayList<>();

    private ActiveSimi() { }

    public static void load(String... files) {
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

    private static void run(String source) {
        Map<String, NativeModulesManager> nativeModulesManagers = new HashMap<>();
        if (interpreter == null) {
            nativeModulesManagers.put("jar", new JavaNativeModulesManager());
            nativeModulesManagers.put("framework", new CocoaNativeModulesManager());

            interpreter = new Interpreter(nativeModulesManagers.values());
        } else {
            for (NativeModulesManager manager : interpreter.nativeModulesManagers) {
                if (manager instanceof JavaNativeModulesManager) {
                    nativeModulesManagers.put("jar", manager);
                } else if (manager instanceof CocoaNativeModulesManager) {
                    nativeModulesManagers.put("framework", manager);
                }
            }
        }

        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanImports(scanner.scanTokens(true), nativeModulesManagers);
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
        if (hadError) return;

        interpreter.interpret(statements);
    }

    private static SimiProperty runExpression(String expression) {
        List<Token> tokens  = new Scanner(expression).scanTokens(true);
        List<Stmt> statements = new Parser(tokens).parse();
        return interpreter.interpret(statements);
    }

    private static List<Token> scanImports(List<Token> input,
                                           Map<String, NativeModulesManager> nativeModulesManagers) {
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
            if (resolvedImports.contains(location)) {
                continue;
            }
            resolvedImports.add(location);
            Path path = Paths.get(location);
            String pathString = path.toString().toLowerCase();
            if (pathString.endsWith(".simi")) {
                List<Token> tokens = new Scanner(readFile(location)).scanTokens(false);
                result.addAll(scanImports(tokens, nativeModulesManagers));
            } else if (nativeModulesManagers != null) {
                String extension = pathString.substring(pathString.lastIndexOf('.') + 1);
                NativeModulesManager manager = nativeModulesManagers.get(extension);
                if (manager != null) {
                    manager.load(importResolver.resolve(path.toString()).toString());
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
        URL resolve(String nativeFileName);
    }

    public interface Callback {
        void done(SimiProperty result);
    }
}
