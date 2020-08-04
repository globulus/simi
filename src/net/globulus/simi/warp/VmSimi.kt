package net.globulus.simi.warp

import net.globulus.simi.Token
import net.globulus.simi.warp.native.NativeModuleLoader
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) {
    val sourceFile = args[0]
    var debugMode = true
    for (i in 1 until args.size) {
        when (args[i]) {
            "-r" -> debugMode = false
        }
    }
    runFile(sourceFile, debugMode)
//    } else if (args.size == 2) {
//        if (args[0] == "-k") {
//            runKotlin(args[1])
//        } else if (args[0] == "-kc") {
//            runKomplier(args[1])
//        }
}

private const val FILE_SIMI = "Simi"

@Throws(IOException::class)
private fun readFile(path: String, prepend: Boolean): String {
    val bytes = Files.readAllBytes(Paths.get(path))
    //    if (prepend) {
//        return "import \"./stdlib/Stdlib.simi\"\n"
//                + content;
//    }
    return String(bytes, Charset.defaultCharset())
}

@Throws(IOException::class)
private fun runFile(path: String, debugMode: Boolean) {
    run(readFile(path, true), debugMode)
    //
//    if (hadError) System.exit(65);
//    if (hadRuntimeError) System.exit(70);
}

@Throws(IOException::class)
private fun run(source: String, debugMode: Boolean) {
    try {
        var time = System.currentTimeMillis()
        print("Scanning and resolving imports...")
        val lexer = Lexer(FILE_SIMI, source, null)
        val tokens = mutableListOf<Token>()
        val lo = lexer.scanTokens(true).apply {
            simiImports.add(0, "./warp_nacelles/core")
        }
        scanImports(Paths.get("").toAbsolutePath(), lo, tokens, mutableListOf())
        println(" " + (System.currentTimeMillis() - time) + " ms")
        time = System.currentTimeMillis()
        println("Compiling...")
        val compiler = Compiler(debugMode)
        val co = compiler.compile(tokens)
        println((System.currentTimeMillis() - time).toString() + " ms")
        time = System.currentTimeMillis()
        val vm = Vm()
        vm.interpret(Fiber(Closure(co)), debugMode)
        println("Running... " + (System.currentTimeMillis() - time) + " ms")
    } catch (e: Exception) { // handles lexer and compiler errors
        println(e.message)
    }
}

@Throws(IOException::class)
private fun scanImports(sourcePath: Path,
                        lo: Lexer.LexerOutput,
                        allTokens: MutableList<Token>,
                        imports: MutableList<String>) {
    val (tokens, simiImports, nativeImports) = lo
    for (import in nativeImports) {
        convertImportToLocation(sourcePath, import,"jar", imports)?.let {
            NativeModuleLoader.load(Paths.get(it).toUri().toURL().toString(), import.substring(import.indexOf('/') + 1), true)
        }
    }
    for (import in simiImports) {
        convertImportToLocation(sourcePath, import, "simi", imports)?.let {
            scanImports(Paths.get(it).parent,
                    Lexer(import, readFile(it, false), null).scanTokens(false),
                    allTokens,
                    imports
            )
        }
    }
    allTokens += tokens
}

private fun convertImportToLocation(sourcePath: Path,
                                    import: String,
                                    extension: String,
                                    imports: MutableList<String>): String? {
    val fileName = "$import.$extension"
    val location = if (import.startsWith("./")) fileName else "$sourcePath/$fileName"
    if (location in imports) {
        return null
    }
    imports += location
    return location
}

//@Throws(IOException::class)
//private fun runKotlin(sourcePath: String) {
//    val source = readFile(sourcePath, true)
//    var time = System.currentTimeMillis()
//    print("Scanning and resolving imports...")
//    val scanner = Scanner(FILE_SIMI, source, null)
//    val tokens = scanner.scanTokens(true)
//    println(" " + (System.currentTimeMillis() - time) + " ms")
//    time = System.currentTimeMillis()
//    println("Compiling...")
//    val compiler = Compiler()
//    val co = compiler.compile(tokens)
//    println((System.currentTimeMillis() - time).toString() + " ms")
//    time = System.currentTimeMillis()
//    println("Replacing content...")
//    val ktContent = readFile("VmTemplate.kt", false)
//    val baString = co.byteCode.joinToString( ", ", "byteArrayOf(", ")")
//    val stString = co.strings.joinToString { "\"${it.replace("\"", "\\\"")}\"" }
//    val replacedKtContent = ktContent
//            .replace("BYTE_CODE", baString)
//            .replace("STRINGS", stString)
//    PrintWriter(FileWriter("kotlinTemp.kt")).use {
//        it.print(replacedKtContent)
//    }
//    println((System.currentTimeMillis() - time).toString() + " ms")
//    time = System.currentTimeMillis()
//
//    println("Compiling kotlin...")
//    var pb = ProcessBuilder("kotlinc",  "kotlinTemp.kt", "-include-runtime", "-d", "simiKt.jar")
//    pb.redirectErrorStream(true)
//    val compileKotlinProcess = pb.start()
//    var reader = BufferedReader(InputStreamReader(compileKotlinProcess.inputStream))
//    var line: String?
//    while (reader.readLine().also { line = it } != null) println("$line")
//    var exitCode = compileKotlinProcess.waitFor()
//    println((System.currentTimeMillis() - time).toString() + " ms")
//
//    if (exitCode != 0) {
//        return
//    }
//
//    time = System.currentTimeMillis()
//    println("Executing...")
//    pb = ProcessBuilder("java",  "-jar", "simiKt.jar")
//    pb.redirectErrorStream(true)
//    val jarProcess = pb.start()
//    reader = BufferedReader(InputStreamReader(jarProcess.inputStream))
//    while (reader.readLine().also { line = it } != null) println("$line")
//    jarProcess.waitFor()
//    println((System.currentTimeMillis() - time).toString() + " ms")
//}
//
//@Throws(IOException::class)
//private fun runKomplier(sourcePath: String) {
//    val source = readFile(sourcePath, true)
//    var time = System.currentTimeMillis()
//    print("Scanning and resolving imports...")
//    val scanner = Scanner(FILE_SIMI, source, null)
//    val tokens = scanner.scanTokens(true)
//    println(" " + (System.currentTimeMillis() - time) + " ms")
//    time = System.currentTimeMillis()
//    println("Compiling...")
//    val compiler = Kompiler()
//    val co = compiler.compile(tokens)
//    println((System.currentTimeMillis() - time).toString() + " ms")
//    time = System.currentTimeMillis()
//    println("Replacing content...")
//    val ktContent = readFile("KompilerTemplate.kt", false)
//    val replacedKtContent = ktContent
//            .replace("INTERPRET", co)
//    PrintWriter(FileWriter("kompilerTemp.kt")).use {
//        it.print(replacedKtContent)
//    }
//    println((System.currentTimeMillis() - time).toString() + " ms")
//    time = System.currentTimeMillis()
//
//    println("Compiling kotlin...")
//    var pb = ProcessBuilder("kotlinc",  "kompilerTemp.kt", "-include-runtime", "-d", "simiKompiler.jar")
//    pb.redirectErrorStream(true)
//    val compileKotlinProcess = pb.start()
//    var reader = BufferedReader(InputStreamReader(compileKotlinProcess.inputStream))
//    var line: String?
//    while (reader.readLine().also { line = it } != null) println("$line")
//    var exitCode = compileKotlinProcess.waitFor()
//    println((System.currentTimeMillis() - time).toString() + " ms")
//
//    if (exitCode != 0) {
//        return
//    }
//
//    time = System.currentTimeMillis()
//    println("Executing...")
//    pb = ProcessBuilder("java",  "-jar", "simiKompiler.jar")
//    pb.redirectErrorStream(true)
//    val jarProcess = pb.start()
//    reader = BufferedReader(InputStreamReader(jarProcess.inputStream))
//    while (reader.readLine().also { line = it } != null) println("$line")
//    jarProcess.waitFor()
//    println((System.currentTimeMillis() - time).toString() + " ms")
//}