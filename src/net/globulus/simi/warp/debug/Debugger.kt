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
    private val ignoredBreakpoints = mapOf<Function, List<Int>>()
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
                val posHasBreakpoint = vm.currentFunction.debugInfo.breakpoints.contains(vm.buffer.position())
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
        val di = frame.closure.function.debugInfo
        val codePointer = frame.getCurrentCodePoint()
        val tokens = di.compiler.tokens.filter { it.file == codePointer.file }
        val highlightTokenIndex = tokens.indexOfFirst { it.line == codePointer.line }
        val table = AsciiTable()
        table.addRule()
        table.addRow(null, null, "BREAKPOINT")
        table.addRule()
        table.addRow("Call stack", "Source code", "Locals").setTextAlignment(TextAlignment.CENTER)
        table.addRule()
        val callStack = getCallStack(focusFrameIndex)
        val source = TokenPatcher.patch(tokens, tokens[highlightTokenIndex])
        val locals = getLocalsForFrame(frame, codePointer, true)
        table.renderer.cwc = CWC_LongestLine()
        table.addRow(callStack.replaceNewlines(), source.replaceNewlines(), locals.replaceNewlines()).setTextAlignment(TextAlignment.LEFT)
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
        val di = frame.closure.function.debugInfo
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
                if (capped && count > MAX_LOCALS) {
                    sb.appendln("...${locals.size - MAX_LOCALS} more locals available, use 'l' to print them all.")
                    break
                }
            }
        }
        return sb.toString()
    }

    private fun getStackForFrame(frame: CallFrame): String {
        val sb = StringBuilder()
        for (i in frame.sp until vm.fiber.sp) {
            sb.appendln("[$i] ${vm.stringify(vm.fiber.stack[i]!!)}")
        }
        return sb.toString()
    }

    private fun readInput(frame: CallFrame, codePointer: CodePointer) {
        print("šdb ('h' for help)> ")
        val input = scanner.nextLine()
        if (input.isEmpty()) {
            return
        }
        when (input[0]) {
            'g' -> { // go to frame
                val loc = input.substring(2).toInt()
                focusFrame = loc
                printFocusFrame()
            }
            'e' -> { // evaluate
                val expr = input.substring(2)
                debuggingOff = true
                vm.push(expr)
                vm.gu(frame.closure.function.debugInfo.compiler)
                val res = vm.stringify(vm.pop())
                debuggingOff = false
                println(res)
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
                println(getStackForFrame(frame))
                readInput(frame, codePointer)
            }
            else -> { // reset the step status
                status = StepStatus.BREAKPOINT
            }
        }
    }

    private val currentCodePoint: CodePointer get() = vm.fiber.frame.getCurrentCodePoint()

    private fun isBreakpointIgnored(): Boolean {
        return ignoredBreakpoints[vm.currentFunction]?.contains(vm.buffer.position()) == true
    }

    private fun String.replaceNewlines() = replace("\n", "<br />")

    private enum class StepStatus {
        BREAKPOINT, INTO, OVER, OUT
    }

    companion object {
        private const val MAX_LOCALS = 5 // maximum number of locals to be printed out in regular mode
    }
}