package com.avabuilds.packetrunner.data

/**
 * A tiny JSON writer and reader for the plain maps, lists, text, numbers and booleans that [com.avabuilds.packetrunner.game.SaveData]
 * and [Note] turn into. It is written by hand so the local save needs no library and can be unit-tested on the computer.
 */
object JsonText {
    fun write(value: Any?): String = StringBuilder().also { append(it, value) }.toString()

    /** Reads JSON text. Gives null for damaged text, so a broken file never crashes the app. */
    fun read(text: String?): Any? {
        if (text.isNullOrBlank()) return null
        return try {
            val p = Parser(text)
            val v = p.value()
            p.skipSpace()
            if (p.atEnd()) v else null
        } catch (_: RuntimeException) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun readMap(text: String?): Map<String, Any?>? = read(text) as? Map<String, Any?>

    private fun append(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is Boolean, is Int, is Long -> out.append(value.toString())
            is Number -> out.append(value.toDouble().takeIf { it.isFinite() }?.toString() ?: "null")
            is String -> quote(out, value)
            is Map<*, *> -> {
                out.append('{')
                value.entries.forEachIndexed { i, e ->
                    if (i > 0) out.append(',')
                    quote(out, e.key.toString())
                    out.append(':')
                    append(out, e.value)
                }
                out.append('}')
            }
            is Iterable<*> -> {
                out.append('[')
                value.forEachIndexed { i, v ->
                    if (i > 0) out.append(',')
                    append(out, v)
                }
                out.append(']')
            }
            else -> quote(out, value.toString())
        }
    }

    private fun quote(out: StringBuilder, s: String) {
        out.append('"')
        for (c in s) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append("\\u%04x".format(c.code))
                else -> out.append(c)
            }
        }
        out.append('"')
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun atEnd() = i >= s.length
        fun skipSpace() { while (i < s.length && s[i].isWhitespace()) i++ }

        fun value(): Any? {
            skipSpace()
            return when (val c = peek()) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                else -> if (c == '-' || c.isDigit()) num() else error("unexpected $c")
            }
        }

        private fun peek(): Char = if (i < s.length) s[i] else error("unexpected end")

        private fun literal(word: String, v: Any?): Any? {
            if (!s.startsWith(word, i)) error("bad literal")
            i += word.length
            return v
        }

        private fun obj(): Map<String, Any?> {
            val map = linkedMapOf<String, Any?>()
            i++ // {
            skipSpace()
            if (peek() == '}') { i++; return map }
            while (true) {
                skipSpace()
                val key = str()
                skipSpace()
                if (peek() != ':') error("expected :")
                i++
                map[key] = value()
                skipSpace()
                when (peek()) {
                    ',' -> i++
                    '}' -> { i++; return map }
                    else -> error("expected , or }")
                }
            }
        }

        private fun arr(): List<Any?> {
            val list = mutableListOf<Any?>()
            i++ // [
            skipSpace()
            if (peek() == ']') { i++; return list }
            while (true) {
                list += value()
                skipSpace()
                when (peek()) {
                    ',' -> i++
                    ']' -> { i++; return list }
                    else -> error("expected , or ]")
                }
            }
        }

        private fun str(): String {
            if (peek() != '"') error("expected string")
            i++
            val sb = StringBuilder()
            while (true) {
                val c = peek()
                i++
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val e = peek()
                        i++
                        when (e) {
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'b' -> sb.append('\b')
                            'u' -> {
                                if (i + 4 > s.length) error("bad escape")
                                sb.append(s.substring(i, i + 4).toInt(16).toChar())
                                i += 4
                            }
                            else -> sb.append(e) // \" \\ \/
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun num(): Number {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            val t = s.substring(start, i)
            return t.toLongOrNull() ?: t.toDouble()
        }
    }
}
