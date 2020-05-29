package net.globulus.simi.warp

import net.globulus.simi.Debugger
import net.globulus.simi.Debugger.ConsoleInterface
import net.globulus.simi.Scanner
import net.globulus.simi.Token
import net.globulus.simi.TokenType
import net.globulus.simi.warp.native.NativeModuleLoader
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths

fun main(args: Array<String>) {
    if (args.size == 1) {
        runFile(args[0])
//    } else if (args.size == 2) {
//        if (args[0] == "-k") {
//            runKotlin(args[1])
//        } else if (args[0] == "-kc") {
//            runKomplier(args[1])
//        }
    }
}

private const val FILE_SIMI = "Simi"
private val debugger = Debugger(ConsoleInterface())

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
private fun runFile(path: String) {
    run(readFile(path, true))
    //
//    if (hadError) System.exit(65);
//    if (hadRuntimeError) System.exit(70);
}

@Throws(IOException::class)
private fun run(source: String) {
    var time = System.currentTimeMillis()
    print("Scanning and resolving imports...")
    val scanner = Scanner(FILE_SIMI, source, null)
    val tokens = scanImports(scanner.scanTokens(true), mutableListOf())
    println(" " + (System.currentTimeMillis() - time) + " ms")
    time = System.currentTimeMillis()
    println("Compiling...")
    val compiler = Compiler()
    val co = compiler.compile(tokens)
    println((System.currentTimeMillis() - time).toString() + " ms")
    co?.let {
        time = System.currentTimeMillis()
        val vm = Vm()
        vm.interpret(it)
        println("Running... " + (System.currentTimeMillis() - time) + " ms")
    }
}

@Throws(IOException::class)
private fun scanImports(input: List<Token>, imports: MutableList<String>): List<Token> {
    val result = mutableListOf<Token>()
    val len = input.size
    var i = 0
    while (i < len) {
        val token = input[i]
        if (token.type != TokenType.IMPORT) {
            i++
            continue
        }
        i++
        val nextToken = input[i]
        if (nextToken.type != TokenType.STRING) {
            i++
            continue
        }
        var location = nextToken.literal.string
        if (imports.contains(location)) {
            i++
            continue
        }
        imports += location
        if (!location.endsWith(".jar") && !location.endsWith(".simi")) {
            location += ".simi"
        }
        if (!location.contains('/')) {
            location = "./$location"
        }
        val path = Paths.get(location)
        var moduleName = path.fileName.toString()
        moduleName = moduleName.substring(0, moduleName.indexOf('.'))
        val pathString = path.toString().toLowerCase()
        if (pathString.endsWith(".jar")) {
            NativeModuleLoader.load(path.toUri().toURL().toString(), moduleName, true)
        } else if (pathString.endsWith(".simi")) {
            val tokens = Scanner(pathString, readFile(location, false), null).scanTokens(false)
            result.addAll(scanImports(tokens, imports))
        }
        i++
    }
    result.addAll(input)
    return result
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