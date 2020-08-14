package net.globulus.simi.warp.debug

import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import net.globulus.simi.tool.TokenPatcher
import net.globulus.simi.warp.CallFrame
import net.globulus.simi.warp.Function
import net.globulus.simi.warp.Vm
import java.util.*

class Debugger(private val vm: Vm) {
    private val scanner = Scanner(System.`in`)
    private var debuggingOff = false
    private val addedBreakpoints = mutableMapOf<Function, MutableSet<Int>>()
    private val ignoredBreakpoints = mutableMapOf<Function, MutableSet<Int>>()
    private var focusFrame: Int = 0
    private var status = StepStatus.BREAKPOINT
    private var triggerPoints = Stack<CodePointer>()
    private var triggerFrames = Stack<Int>()

    fun triggerBreakpoint(isCall: Boolean = false) {
        if (debuggingOff) {
            return
        }
        focusFrame = 0
        when (status) {
            StepStatus.BREAKPOINT -> {
                val pos = currentPosition
                val posHasBreakpoint = vm.currentFunction.debugInfo!!.breakpoints.contains(pos) || addedBreakpoints[vm.currentFunction]?.contains(pos) == true
                if (posHasBreakpoint && isBreakpointIgnored() || !posHasBreakpoint) {
                    return
                }
            }
            StepStatus.INTO -> {
                if (!isCall) {
                    return
                }
                status = StepStatus.BREAKPOINT
            }
            StepStatus.OVER -> {
                if (vm.fiber.fp > triggerFrames.peek()) {
                    return
                }
                if (currentCodePoint == triggerPoints.peek()) {
                    return
                }
                triggerPoints.pop()
                triggerFrames.pop()
                status = StepStatus.BREAKPOINT
            }
            StepStatus.OUT -> {
                if (currentCodePoint != triggerPoints.peek()) {
                    return
                }
                triggerPoints.pop()
                status = StepStatus.BREAKPOINT
            }
        }
        focusFrame = 0
        printFocusFrame()
    }

    private fun printFocusFrame() {
        val focusFrameIndex = vm.fiber.fp - 1 - focusFrame
        val frame = vm.fiber.callFrames[focusFrameIndex]!!
        val di = frame.closure.function.debugInfo!!
        val codePointer = frame.getCurrentCodePoint()
        val tokens = di.compiler.tokens.filter { it.file == codePointer.file }
        val highlightTokenIndex = tokens.indexOfFirst { it.line == codePointer.line }
        val table = AsciiTable()
        table.addRule()
        table.addRow(null, null, null, "BREAKPOINT")
        table.addRule()
        table.addRow("Call stack", "Source code", "Locals", "Fiber stack").setTextAlignment(TextAlignment.CENTER)
        table.addRule()
        val callStack = getCallStack(focusFrameIndex)
        val source = TokenPatcher.patch(tokens, tokens[highlightTokenIndex])
        val locals = getLocalsForFrame(frame, codePointer, true)
        val stack = getStackForFrame(frame, true)
        table.renderer.cwc = CWC_LongestLine()
        table.addRow(callStack.replaceNewlines(), source.replaceNewlines(), locals.replaceNewlines(),
                stack.replaceNewlines()).setTextAlignment(TextAlignment.LEFT)
        table.addRule()
        println(table.render())
        readInput(frame, codePointer)
    }

    private fun getCallStack(focusFrameIndex: Int): String {
        val sb = StringBuilder()
        for (i in vm.fiber.fp - 1 downTo 0) {
            if (i == focusFrameIndex) {
                sb.append("* ") // mark the focus frame
            }
            sb.appendln(vm.fiber.callFrames[i])
        }
        return sb.toString()
    }

    private fun getLocalsForFrame(frame: CallFrame, codePointer: CodePointer, capped: Boolean): String {
        val sb = StringBuilder()
        val di = frame.closure.function.debugInfo!!
        val locals = di.locals.sortedByDescending { it.second.start.line }
        if (locals.isEmpty()) { // Print the stack instead
            for (i in frame.sp until vm.fiber.sp) {
                sb.appendln("[$i] ${vm.stringify(vm.fiber.stack[i]!!)}")
            }
        } else {
            var count = 0
            for (pair in locals) { // reverse locals so that the most recently used one are at the top
                val lifetime = pair.second
                if (lifetime.start.file == codePointer.file && lifetime.start.line > codePointer.line) {
                    continue // this one wasn't declared yet
                }
                if (lifetime.end?.file == codePointer.file && lifetime.end?.line ?: -1 < codePointer.line) {
                    continue // this one's already dead
                }
                val local = pair.first
                vm.fiber.stack[frame.sp + local.sp]?.let {
                    sb.appendln("${local.name} = ${vm.stringify(it)}")
                    count++
                } ?: break // if we reached into the null territory of the stack (shouldn't happen, but still), break
                if (capped && count == MAX_LOCALS) {
                    sb.appendln("...${locals.size - MAX_LOCALS} more locals available, use 'l' to print them all.")
                    break
                }
            }
        }
        return sb.toString()
    }

    private fun getStackForFrame(frame: CallFrame, capped: Boolean): String {
        val sb = StringBuilder()
        var count = 0
        for (i in vm.fiber.sp - 1 downTo frame.sp) {
            sb.appendln("[$i] ${vm.stringify(vm.fiber.stack[i]!!).limitValue()}")
            count++
            if (capped && count == MAX_STACK_ITEMS) {
                sb.appendln("...${vm.fiber.sp - frame.sp - MAX_STACK_ITEMS} more stack items available, use 's' to print them all.")
                break
            }
        }
        return sb.toString()
    }

    private fun readInput(frame: CallFrame, codePointer: CodePointer) {
        print("šdb ('h' for help)> ")
        val line = scanner.nextLine()
        when (line[0]) {
            'h' -> { // print help
                println(HELP)
                readInput(frame, codePointer)
            }
            'g' -> { // go to frame
                val loc = line.substring(2).toInt()
                focusFrame = loc
                printFocusFrame()
            }
            'e' -> { // evaluate
                val expr = line.substring(2)
                debuggingOff = true
                vm.push(expr)
                vm.gu(frame.closure.function.debugInfo!!.compiler)
                val res = vm.stringify(vm.pop())
                debuggingOff = false
                println(res)
                readInput(frame, codePointer)
            }
            'p' -> { // print the ivic dump of the stack value
                val loc = line.substring(2).toInt()
                println(vm.ivic(vm.fiber.stack[loc]!!))
                readInput(frame, codePointer)
            }
            'i' -> { // step into
                status = StepStatus.INTO
                triggerPoints.push(currentCodePoint)
            }
            'v' -> { // step over
                status = StepStatus.OVER
                triggerFrames.push(vm.fiber.fp)
                triggerPoints.push(currentCodePoint)
            }
            'o' -> { // step out
                status = StepStatus.OUT
            }
            'l' -> { // print all locals
                println(getLocalsForFrame(frame, codePointer, false))
                readInput(frame, codePointer)
            }
            's' -> { // print the stack for the current frame
                println(getStackForFrame(frame, false))
                readInput(frame, codePointer)
            }
            'a' -> { // add current position as breakpoint

                addedBreakpoints.addToSet(vm.currentFunction, currentPosition)
                println("Breakpoint added for $codePointer.")
                readInput(frame, codePointer)
            }
            'r' -> { // remove current position as breakpoint
                val pos = currentPosition
                if (addedBreakpoints[vm.currentFunction]?.contains(pos) == true) {
                    addedBreakpoints[vm.currentFunction]?.remove(pos)
                    println("Breakpoint removed for $codePointer.")
                } else {
                    ignoredBreakpoints.addToSet(vm.currentFunction, pos)
                    println("Breakpoint ignored for $codePointer.")
                }
                readInput(frame, codePointer)
            }
            else -> { // reset the step status
                status = StepStatus.BREAKPOINT
            }
        }
    }

    private val currentCodePoint: CodePointer get() = vm.fiber.frame.getCurrentCodePoint()

    private val currentPosition: Int get() = vm.buffer.position()

    private fun isBreakpointIgnored(): Boolean {
        return ignoredBreakpoints[vm.currentFunction]?.contains(vm.buffer.position()) == true
    }

    private fun String.replaceNewlines() = replace("\n", "<br />")

    private fun String.limitValue() = if (length > MAX_VALUE_LENGTH) substring(0, MAX_VALUE_LENGTH) + "..." else this

    private fun <T> MutableMap<Function, MutableSet<T>>.addToSet(key: Function, item: T) {
        (get(key) ?: mutableSetOf<T>().also {
            put(key, it)
        }).add(item)
    }

    private enum class StepStatus {
        BREAKPOINT, INTO, OVER, OUT
    }

    companion object {
        private const val MAX_LOCALS = 5 // maximum number of locals to be printed out in regular mode
        private const val MAX_STACK_ITEMS = 5 // maximum number of stack items to be printed out in regular mode
        private const val MAX_VALUE_LENGTH = 100

        private const val HELP = """
            Commands:
                g index - (g)o to call frame at index.
                e expression - (e)valuate the provided expression (basically invokes gu "expression").
                p index - (p)rints the value at the stack index (basically does ivic stack[index]).
                i - step (i)nto.
                v - step o(v)er.
                o - step (o)ut.
                l - print all (l)ocals.
                s - print the entire fiber (s)tack.
                a - add a breakpoint for current line.
                r - removes the breakpoint at current line.
        """
    }
}