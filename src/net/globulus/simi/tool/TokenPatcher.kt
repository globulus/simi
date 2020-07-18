package net.globulus.simi.tool

import net.globulus.simi.Token
import net.globulus.simi.TokenType.*

object TokenPatcher {
    private const val SURROUNDING_LINES_COUNT = 2

    fun patch(tokens: List<Token>, highlighted: Token): String {
        val targetLine = highlighted.line
        val sb = StringBuilder()
        var totalLenByLineStart = 0
        var highlightPosition = 0
        var printLineNumber = true
        for (token in tokens) {
            if (token.file != highlighted.file) {
                continue
            }
            val line = token.line
            if (line < targetLine - SURROUNDING_LINES_COUNT) {
                continue
            } else if (line > targetLine + SURROUNDING_LINES_COUNT) {
                break
            }
            sb.append(if (printLineNumber) "[$line] " else spaceBefore(token))
            val isNewline = token.type == NEWLINE
            if (token == highlighted) {
                highlightPosition = sb.length - totalLenByLineStart - 1
            } else if (isNewline) {
                if (line == targetLine - 1) {
                    totalLenByLineStart = sb.length
                } else if (line == targetLine) {
                    sb.append("\n")
                            .append(if (highlightPosition == -1) "" else " ".repeat(highlightPosition))
                            .append("^")
                }
            }
            sb.append(tokenCode(token))
            printLineNumber = isNewline
        }
        return sb.toString()
    }

    internal fun tokenCode(token: Token) = try {
        token.type.toCode()
    } catch (e: Exception) {
        token.lexeme
    }

    internal fun spaceBefore(token: Token) = when (token.type) {
        DOT, DOT_DOT, DOT_DOT_DOT, LEFT_PAREN, RIGHT_PAREN, LEFT_BRACKET, RIGHT_BRACKET -> ""
        else -> " "
    }
}