package net.globulus.simi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Simi {

  private static Interpreter interpreter;
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: simi [script]");
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
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
      run(reader.readLine() + "\n");
      hadError = false;
    }
  }

  private static void run(String source) throws IOException {
//    Environment environment = new Environment();
//    Map<String, String> strings = new HashMap<>();
//    Map<String, Map<String, ?>> vals = new HashMap<>();
    long time = System.currentTimeMillis();
//    environment.assign(Token.named("aaa"), new SimiValue.String("aaaa"), false);
//    String str = environment.tryGet("aaa").getString();
//    System.out.println(" " + (System.currentTimeMillis() - time) + " ms");
//    time = System.currentTimeMillis();
//    strings.put("aaa", "aaaa");
//    vals.put("aaa", strings);
//    String str2 = (String) vals.get("aaa").get("aaa");
//    System.out.println(" " + (System.currentTimeMillis() - time) + " ms");

      NativeModulesManager nativeModulesManager = new NativeModulesManager();
      List<String> imports = new ArrayList<>();
//      time = System.currentTimeMillis();
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

  static void error(int line, String message) {
    report(line, "", message);
  }

  private static void report(int line, String where, String message) {
    System.err.println(
        "[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() +
        "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }
}
