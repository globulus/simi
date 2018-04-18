package net.globulus.simi;

import net.globulus.simi.api.SimiProperty;
import net.globulus.simi.api.SimiValue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ActiveSimi {

    private static Interpreter interpreter;
    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    private ActiveSimi() { }

    public static void load(String source) throws IOException {
        ErrorHub.sharedInstance().removeWatcher(WATCHER);
        ErrorHub.sharedInstance().addWatcher(WATCHER);
        run(source);
    }

    public static SimiProperty eval(String className, String methodName, SimiProperty... params) {
        StringBuilder sb = new StringBuilder();
        if (className != null) {
            sb.append(className).append('.');
        }
        sb.append(methodName).append('(');
        boolean first = true;
        for (SimiProperty param : params) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            SimiValue value = param.getValue();
            String s;
            if (value instanceof SimiValue.String) {
                s = "\"" + value.toString() + "\"";
            } else {
                s = value.toString();
            }
            sb.append(s);
        }
        sb.append(')');
        return runExpression(sb.toString());
    }

    private static String readFile(String path, boolean prepend) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String content = new String(bytes, Charset.defaultCharset());
        return content;
    }

    private static void run(String source) throws IOException {
        NativeModulesManager nativeModulesManager = new NativeModulesManager();
        List<String> imports = new ArrayList<>();

        long time = System.currentTimeMillis();
        System.out.print("Scanning and resolving imports...");
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanImports(scanner.scanTokens(true), imports, nativeModulesManager);
        System.out.println(" " + (System.currentTimeMillis() - time) + " ms");
        time = System.currentTimeMillis();
        System.out.print("Parsing...");
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        interpreter = new Interpreter(nativeModulesManager);

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
                                           NativeModulesManager nativeModulesManager) throws IOException {
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
            if (pathString.endsWith(".jar")) {
                nativeModulesManager.loadJar(path.toUri().toURL());
            } else if (pathString.endsWith(".simi")) {
                List<Token> tokens = new Scanner(readFile(location, false)).scanTokens(false);
                result.addAll(scanImports(tokens, imports, nativeModulesManager));
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
}
