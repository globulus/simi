package net.globulus.simi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Simi {

  private static final String FILE_SIMI = "Simi";

  private static Interpreter interpreter;
  private static Debugger debugger = new Debugger(new Debugger.ConsoleInterface());
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    ErrorHub.sharedInstance().addWatcher(WATCHER);
    if (args.length > 1) {
      System.out.println("Usage: simi [script]");
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
    ErrorHub.sharedInstance().removeWatcher(WATCHER);
  }

  private static String readFile(String path, boolean prepend) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    String content = new String(bytes, Charset.defaultCharset());
    if (prepend) {
        return "import \"./stdlib/Stdlib.simi\"\n"
                + content;
    }
    return content;
  }

  private static void runFile(String path) throws IOException {
    run(readFile(path, true));

    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
  }

  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) { // [repl]
      System.out.print("> ");
      run(reader.readLine());
      hadError = false;
    }
  }

  private static void run(String source) throws IOException {
      NativeModulesManager nativeModulesManager = new JavaNativeModulesManager();
      List<String> imports = new ArrayList<>();

      long time = System.currentTimeMillis();
      System.out.print("Scanning and resolving imports...");
    Scanner scanner = new Scanner(FILE_SIMI, source, debugger);
    List<Token> tokens = scanImports(scanner.scanTokens(true), imports, nativeModulesManager);
    System.out.println(" " + (System.currentTimeMillis() - time) + " ms");
    time = System.currentTimeMillis();
    System.out.print("Parsing...");

    interpreter = new Interpreter(Collections.singletonList(nativeModulesManager), debugger);
    ErrorHub.sharedInstance().setInterpreter(interpreter);

    Parser parser = new Parser(tokens, debugger);
    List<Stmt> statements = parser.parse();

    // Stop if there was a syntax error.
    if (hadError) return;

    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);

    System.out.println(" " + (System.currentTimeMillis() - time) + " ms");
    time = System.currentTimeMillis();
    // Stop if there was a resolution error.
    if (hadError) return;

    interpreter.interpret(statements);
    System.out.println("Interpreting... " + (System.currentTimeMillis() - time) + " ms");
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
          nativeModulesManager.load(path.toUri().toURL().toString(), true);
      } else if (pathString.endsWith(".simi")) {
          List<Token> tokens = new Scanner(pathString, readFile(location, false), debugger).scanTokens(false);
          result.addAll(scanImports(tokens, imports, nativeModulesManager));
      }
    }
    result.addAll(input);
    return result;
  }

  private static final ErrorWatcher WATCHER = new ErrorWatcher() {
    @Override
    public void report(String file, int line, String where, String message) {
      System.err.println("[\"" + file + "\" line " + line + "] Error" + where + ": " + message);
      hadError = true;
    }

    @Override
    public void runtimeError(RuntimeError error) {

      System.err.println(error.getMessage() +
              "\n[\"" + error.token.file + "\" line " + error.token.line + "]");
      hadRuntimeError = true;
    }
  };
}
