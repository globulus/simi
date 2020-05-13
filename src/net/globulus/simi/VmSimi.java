package net.globulus.simi;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class VmSimi {

  private static final String FILE_SIMI = "Simi";
  private static Debugger debugger = new Debugger(new Debugger.ConsoleInterface());

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: simi [script]");
    } else if (args.length == 1) {
      runFile(args[0]);
    }
  }

  private static String readFile(String path, boolean prepend) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    String content = new String(bytes, Charset.defaultCharset());
//    if (prepend) {
//        return "import \"./stdlib/Stdlib.simi\"\n"
//                + content;
//    }
    return content;
  }

  private static void runFile(String path) throws IOException {
    run(readFile(path, true));
//
//    if (hadError) System.exit(65);
//    if (hadRuntimeError) System.exit(70);
  }

  private static void run(String source) throws IOException {

      long time = System.currentTimeMillis();
      System.out.print("Scanning and resolving imports...");
    Scanner scanner = new Scanner(FILE_SIMI, source, null);
    List<Token> tokens = scanner.scanTokens(true);
    System.out.println(" " + (System.currentTimeMillis() - time) + " ms");
    time = System.currentTimeMillis();
    System.out.println("Compiling...");

    Compiler compiler = new Compiler(tokens);
    Compiler.CompilerOutput co = compiler.compile();

    System.out.println((System.currentTimeMillis() - time) + " ms");
    time = System.currentTimeMillis();

    Vm vm = new Vm();
    vm.interpret(co);
    System.out.println("Running... " + (System.currentTimeMillis() - time) + " ms");
  }
}
