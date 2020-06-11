package net.globulus.simi.tool

import net.globulus.simi.Token
import net.globulus.simi.TokenType

object TokenPatcher {
    private const val SURROUNDING_LINES_COUNT = 2

    fun patch(tokens: List<Token>, highlighted: Token): String {
        val targetLine = highlighted.line
        val sb = StringBuilder()
        var totalLenByLineStart = 0
        var highlightPosition = 0
        for (token in tokens) {
            val line = token.line
            if (line < targetLine - SURROUNDING_LINES_COUNT) {
                continue
            } else if (line > targetLine + SURROUNDING_LINES_COUNT) {
                break
            }
            val isNewline = token.type == TokenType.NEWLINE
            if (token == highlighted) {
                highlightPosition = sb.length - totalLenByLineStart - 1
            } else if (isNewline) {
                if (line == targetLine) {
                    totalLenByLineStart = sb.length
                } else if (line == targetLine + 1) {
                    sb.append("\n")
                            .append(if (highlightPosition == -1) "" else " ".repeat(highlightPosition))
                            .append("^")
                }
            }
            sb.append(tokenCode(token))
                    .append(if (isNewline) "[$line] " else " ")
        }
        return sb.toString()
    }

    private fun tokenCode(token: Token) = try {
        token.type.toCode()
    } catch (e: Exception) {
        token.lexeme
    }
}