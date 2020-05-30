package net.globulus.simi.warp

import net.globulus.simi.*
import net.globulus.simi.TokenType.*
import net.globulus.simi.api.SimiValue

class Lexer(private val fileName: String, 
            private val source: String, 
            private val debugger: Debugger?
) {
    private val tokens = mutableListOf<Token>()
    private val simiImports = mutableListOf<String>()
    private val nativeImports = mutableListOf<String>()
    private var start = 0
    private var current = 0
    private var line = 1
    private var stringInterpolationParentheses = 0
    private val lastStringOpener = '"'

    fun scanTokens(addEof: Boolean): LexerOutput {
        while (!isAtEnd) {
            // We are at the beginning of the next lexeme.
            start = current
            try {
                scanToken()
            } catch (ignored: Exception) { }
        }
        if (addEof) {
            tokens.add(Token(EOF, "", null, line, fileName))
        }
        return LexerOutput(tokens, simiImports, nativeImports)
    }

    private fun scanToken() {
        when (val c = advance()) {
            '(' -> {
                addToken(LEFT_PAREN)
                if (stringInterpolationParentheses > 0) {
                    stringInterpolationParentheses++
                }
            }
            ')' -> {
                addToken(RIGHT_PAREN)
                if (stringInterpolationParentheses > 0) {
                    stringInterpolationParentheses--
                    if (stringInterpolationParentheses == 0) {
                        addToken(PLUS)
                        string(lastStringOpener, true)
                    }
                }
            }
            '[' -> addToken(LEFT_BRACKET)
            ']' -> addToken(RIGHT_BRACKET)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> {
                if (match('.')) {
                    if (match('.')) {
                        addToken(DOT_DOT_DOT)
                    } else {
                        addToken(DOT_DOT)
                    }
                } else {
                    addToken(DOT)
                }
            }
            ':' -> addToken(COLON)
            '@' -> {
                tokens.add(Token(SELF, Constants.SELF, null, line, fileName))
                addToken(DOT)
            }
            '?' -> {
                if (match('?')) {
                    if (match('=')) {
                        addToken(QUESTION_QUESTION_EQUAL)
                    } else {
                        addToken(QUESTION_QUESTION)
                    }
                } else {
                    addToken(QUESTION)
                }
            }
            '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> {
                if (match('>')) {
                    addToken(LESS_GREATER)
                } else if (match('=')) {
                    addToken(LESS_EQUAL)
                } else {
                    addToken(LESS)
                }
            }
            '!' -> {
                if (match('!')) {
                    addToken(BANG_BANG)
                } else if (match('=')) {
                    addToken(BANG_EQUAL)
                } else {
                    addToken(BANG)
                }
            }
            '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)
            '+' -> addToken(if (match('=')) PLUS_EQUAL else PLUS)
            '-' -> addToken(if (match('=')) MINUS_EQUAL else MINUS)
            '/' -> {
                if (match('/')) {
                    if (match('=')) {
                        addToken(SLASH_SLASH_EQUAL)
                    } else {
                        addToken(SLASH_SLASH)
                    }
                } else if (match('=')) {
                    addToken(SLASH_EQUAL)
                } else {
                    addToken(SLASH)
                }
            }
            '*' -> {
                if (match('*')) {
                    addToken(STAR_STAR)
                } else if (match('=')) {
                    addToken(STAR_EQUAL)
                } else {
                    addToken(STAR)
                }
            }
            '%' -> {
                if (match('%')) {
                    addToken(MOD_MOD)
                } else if (match('=')) {
                    addToken(MOD_EQUAL)
                } else {
                    addToken(MOD)
                }
            }
            '$' -> {
                if (match('=')) {
                    addToken(DOLLAR_EQUAL)
                } else if (match('(')) {
                    addToken(DOLLAR_LEFT_PAREN)
                } else if (match('[')) {
                    addToken(DOLLAR_LEFT_BRACKET)
                } else {
                    identifier()
                }
            }
            '#' -> comment()
            '\\' -> {
                if (match('\n')) {
                    line++
                }
            }
            '\n' -> {
                line++
                addToken(NEWLINE)
            }
            ';' -> addToken(NEWLINE)
            else -> if (isStringDelim(c)) {
                string(c, true)
            } else if (isDigit(c)) {
                number()
            } else if (isAlpha(c)) {
                identifier()
            } else if (c !in WHITESPACES) {
                throw error("Unexpected character.")
            }
        }
    }

    private fun comment() {
        if (match('*')) { // Multi line
            while (!matchAll("*#")) {
                if (peek == '\n') {
                    line++
                }
                advance()
            }
        } else { // Single line
            // A comment goes until the end of the line.
            while (peek != '\n' && !isAtEnd) {
                advance()
            }
            if (debugger != null) {
                val comment = source.substring(start + 1, current)
                if (comment.trim { it <= ' ' }.startsWith(Debugger.BREAKPOINT_LEXEME)) {
                    val size = tokens.size
                    for (i in size - 1 downTo 0) {
                        val token = tokens[i]
                        if (token.line == line) {
                            token.hasBreakpoint = true
                        } else {
                            break
                        }
                    }
                }
            }
        }
    }

    private fun identifier() {
        while (isAlphaNumeric(peek)) {
            advance()
        }

        // See if the identifier is a reserved word.
        val text = source.substring(start, current)
        var type = keywords[text]
        if (type == NOT && matchPeek(IN)) {
            type = NOTIN
        } else if (type == IS && matchPeek(NOT)) {
            type = ISNOT
        } else if (type == CLASS) {
            val candidateText = text + peek
            val candidateType = keywords[candidateText]
            if (candidateType != null) {
                type = candidateType
                advance()
            }
        } else if (type == IMPORT) {
            if (matchPeek(NATIVE)) {
                val stringOpener = matchWhiteSpacesUntilStringOpener()
                start = current - 1
                nativeImports += string(stringOpener,false)
            } else {
                val curr = current
                try {
                    val stringOpener = matchWhiteSpacesUntilStringOpener()
                    start = current - 1
                    simiImports += string(stringOpener,false)
                } catch (e: Exception) {
                    current = curr
                }
            }
        } else if (type == null) {
            type = IDENTIFIER
        }
        addToken(type)
    }

    private fun number() {
        while (isDigitOrUnderscore(peek)) {
            advance()
        }
        // Look for a fractional part.
        if (peek == '.' && isDigit(peekNext)) {
            // Consume the "."
            advance()
            while (isDigitOrUnderscore(peek)) {
                advance()
            }
        }
        // Exp notation
        if (peek == 'e' || peek == 'E') {
            if (isDigit(peekNext)) {
                advance()
            } else if (peekNext == '+' || peekNext == '-') {
                advance()
                advance()
            } else {
                throw error("Expected a digit or + or - after E!")
            }
            while (isDigitOrUnderscore(peek)) {
                advance()
            }
        }
        val numberString = source.substring(start, current).replace("_", "")
        val literal: SimiValue.Number
        literal = try {
            SimiValue.Number(numberString.toLong())
        } catch (e: NumberFormatException) {
            SimiValue.Number(numberString.toDouble())
        }
        addToken(NUMBER, literal)
    }

    private fun string(opener: Char, addTokenIfNotInterpolation: Boolean): String {
        while (peek != opener && !isAtEnd) {
            if (peek == '\n') {
                line++
            } else if (peek == '\\') {
                val next = peekNext
                if (next == opener) {
                    advance()
                } else if (next == '(') { // String interpolation
                    val valueSoFar = escapedString(start + 1, current)
                    addToken(STRING, SimiValue.String(valueSoFar))
                    addToken(PLUS)
                    advance() // Skip the \
                    advance() // Skip the (
                    addToken(LEFT_PAREN)
                    stringInterpolationParentheses = 1
                    return ""
                }
            }
            advance()
        }

        // Unterminated string.
        if (isAtEnd) {
            throw error("Unterminated string.")
        }

        // The closing ".
        advance()

        // Trim the surrounding quotes.
        val value = escapedString(start + 1, current - 1)
        if (addTokenIfNotInterpolation) {
            addToken(STRING, SimiValue.String(value))
        }
        return value
    }

    private fun escapedString(start: Int, stop: Int): String {
        val backslashBackslashNReplacement = "\\\\r"
        return source.substring(start, stop)
                .replace("\\\\n", backslashBackslashNReplacement)
                .replace("\\n", "\n")
                .replace(backslashBackslashNReplacement, "\\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
    }

    private fun keywordString(type: TokenType): String? {
        for (s in keywords.keys) {
            if (keywords[s] == type) {
                return s
            }
        }
        return null
    }

    private fun matchPeek(type: TokenType): Boolean {
        val keyword = keywordString(type) ?: return false
        val len = keyword.length
        val end = current + len + 1
        if (end < source.length && source.substring(current + 1, end) == keyword) {
            current = end
            return true
        }
        return false
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun matchAll(expected: String): Boolean {
        val end = current + expected.length
        if (end >= source.length) {
            return false
        }
        if (source.substring(current, end) != expected) {
            return false
        }
        current = end
        return true
    }

    private fun matchAllWhiteSpaces() {
        while (advance() in WHITESPACES) { }
        current-- // Skip back to the first char that's not a whitespace
    }

    private fun matchWhiteSpacesUntilStringOpener(): Char {
        matchAllWhiteSpaces()
        val c = advance()
        if (isStringDelim(c)) {
            return c
        } else {
            throw error("Expected a string!")
        }
    }

    private val peek: Char get() = if (isAtEnd) '\u0000' else source[current]
    private val peekNext: Char get() = if (current + 1 >= source.length) '\u0000' else source[current + 1]

    private fun isAlpha(c: Char): Boolean {
        return c in 'a'..'z' || c in 'A'..'Z' || c == '_'
    }

    private fun isAlphaNumeric(c: Char): Boolean {
        return isAlpha(c) || isDigit(c)
    }

    private fun isDigit(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun isDigitOrUnderscore(c: Char): Boolean {
        return isDigit(c) || c == '_'
    }

    private fun isStringDelim(c: Char): Boolean {
        return c == '"' || c == '\''
    }

    private val isAtEnd: Boolean
        get() = current >= source.length

    private fun advance(): Char {
        current++
        return source[current - 1]
    }

    private fun addToken(type: TokenType, literal: SimiValue? = null) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line, fileName))
    }

    private fun error(message: String): Exception {
        ErrorHub.sharedInstance().error(Constants.EXCEPTION_SCANNER, fileName, line, message)
        return RuntimeException(message)
    }

    data class LexerOutput(val tokens: List<Token>,
                           val simiImports: List<String>,
                           val nativeImports: List<String>
    )

    companion object {
        val keywords = mapOf(
                "and" to AND,
                "break" to BREAK,
                "class" to CLASS,
                "class_" to CLASS_FINAL,
                "class$" to CLASS_OPEN,
                "continue" to CONTINUE,
                "def" to DEF,
                "do" to DO,
                "else" to ELSE,
                "false" to FALSE,
                "for" to FOR,
                "gu" to GU,
                "if" to IF,
                "import" to IMPORT,
                "in" to IN,
                "is" to IS,
                "ivic" to IVIC,
                "native" to NATIVE,
                "nil" to NIL,
                "not" to NOT,
                "or" to OR,
                "print" to PRINT,
                "rescue" to RESCUE,
                "return" to RETURN,
                Constants.SELF to SELF,
                Constants.SUPER to SUPER,
                "true" to TRUE,
                "when" to WHEN,
                "while" to WHILE,
                "yield" to YIELD
        )
        val WHITESPACES = arrayOf( ' ', '\r', '\t')
    }
}