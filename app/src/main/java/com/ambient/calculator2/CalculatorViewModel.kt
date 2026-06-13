package com.ambient.calculator2

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.math.*

class CalculatorViewModel : ViewModel() {
    var display by mutableStateOf("")
        private set

    var result by mutableStateOf("")
        private set

    var isAdvancedMode by mutableStateOf(false)
        private set

    var history by mutableStateOf(listOf<HistoryItem>())
        private set

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Number -> enterNumber(action.number)
            is CalculatorAction.Operator -> enterOperator(action.operator)
            is CalculatorAction.Clear -> clear()
            is CalculatorAction.ClearEntry -> clearEntry()
            is CalculatorAction.Backspace -> backspace()
            is CalculatorAction.Decimal -> enterDecimal()
            is CalculatorAction.Calculate -> calculate()
            is CalculatorAction.ToggleMode -> isAdvancedMode = !isAdvancedMode
            is CalculatorAction.Function -> enterFunction(action.function)
            is CalculatorAction.Constant -> enterConstant(action.constant)
            is CalculatorAction.Sign -> changeSign()
            is CalculatorAction.ClearHistory -> history = emptyList()
            is CalculatorAction.SelectHistory -> {
                display = action.item.expression
                result = action.item.result
            }
        }
    }

    private fun enterNumber(number: Int) {
        if (result.isNotEmpty() && display == result) {
            display = ""
            result = ""
        }
        display += number.toString()
    }

    private fun enterOperator(operator: String) {
        if (display.isEmpty()) {
            if (operator == "-") display += operator
            return
        }
        if (isOperator(display.last())) {
            display = display.dropLast(1) + operator
        } else if (display.last() != '(') {
            display += operator
        }
    }

    private fun enterFunction(function: String) {
        if (display.isNotEmpty() && (display.last().isDigit() || display.last() == ')' || display.last() == 'π' || display.last() == 'e')) {
            display += "*"
        }
        display += "$function("
    }

    private fun enterConstant(constant: String) {
        if (display.isNotEmpty() && (display.last().isDigit() || display.last() == ')' || display.last() == 'π' || display.last() == 'e')) {
            display += "*"
        }
        display += constant
    }

    private fun enterDecimal() {
        if (display.isEmpty() || isOperator(display.last()) || display.last() == '(') {
            display += "0."
            return
        }
        val lastPart = display.split(Regex("[+\\-*/^()%]")).last()
        if (lastPart.isEmpty()) {
            display += "0."
        } else if (!lastPart.contains(".")) {
            display += "."
        }
    }

    private fun clear() {
        display = ""
        result = ""
    }

    private fun clearEntry() {
        display = ""
    }

    private fun backspace() {
        if (display.isNotEmpty()) {
            display = display.dropLast(1)
        }
    }

    private fun changeSign() {
        if (display.startsWith("-")) {
            display = display.substring(1)
        } else {
            display = "-$display"
        }
    }

    private fun calculate() {
        if (display.isEmpty()) return
        try {
            val evalResult = ExpressionEvaluator(display).evaluate()
            val formattedResult = formatResult(evalResult)
            
            // Add to history
            history = listOf(HistoryItem(display, formattedResult)) + history
            
            result = formattedResult
            display = formattedResult // Allow further operations on result
        } catch (e: Exception) {
            result = "Error"
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            "%.8f".format(value).trimEnd('0').trimEnd('.')
        }
    }

    private fun isOperator(char: Char): Boolean {
        return char in "+-*/^%"
    }
}

data class HistoryItem(
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class CalculatorAction {
    data class Number(val number: Int) : CalculatorAction()
    data class Operator(val operator: String) : CalculatorAction()
    object Clear : CalculatorAction()
    object ClearEntry : CalculatorAction()
    object Backspace : CalculatorAction()
    object Decimal : CalculatorAction()
    object Calculate : CalculatorAction()
    object ToggleMode : CalculatorAction()
    data class Function(val function: String) : CalculatorAction()
    data class Constant(val constant: String) : CalculatorAction()
    object Sign : CalculatorAction()
    object ClearHistory : CalculatorAction()
    data class SelectHistory(val item: HistoryItem) : CalculatorAction()
}

class ExpressionEvaluator(private val expression: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        ch = if (++pos < expression.length) expression[pos].toInt() else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.toInt()) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun evaluate(): Double {
        nextChar()
        val x = parseExpression()
        if (pos < expression.length) throw RuntimeException("Unexpected: " + ch.toChar())
        return x
    }

    private fun parseExpression(): Double {
        var x = parseTerm()
        while (true) {
            if (eat('+'.toInt())) x += parseTerm()
            else if (eat('-'.toInt())) x -= parseTerm()
            else return x
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.toInt())) x *= parseFactor()
            else if (eat('/'.toInt())) x /= parseFactor()
            else return x
        }
    }

    private fun parseFactor(): Double {
        if (eat('+'.toInt())) return parseFactor()
        if (eat('-'.toInt())) return -parseFactor()

        var x: Double
        val startPos = this.pos
        if (eat('('.toInt())) {
            x = parseExpression()
            eat(')'.toInt())
        } else if ((ch >= '0'.toInt() && ch <= '9'.toInt()) || ch == '.'.toInt()) {
            while ((ch >= '0'.toInt() && ch <= '9'.toInt()) || ch == '.'.toInt()) nextChar()
            x = expression.substring(startPos, this.pos).toDouble()
        } else if (ch >= 'a'.toInt() && ch <= 'z'.toInt() || ch == 'π'.toInt() || ch == 'e'.toInt()) {
            while (ch >= 'a'.toInt() && ch <= 'z'.toInt() || ch == 'π'.toInt() || ch == 'e'.toInt()) nextChar()
            val func = expression.substring(startPos, this.pos)
            if (func == "π") {
                x = PI
            } else if (func == "e") {
                x = E
            } else {
                x = parseFactor()
                x = when (func) {
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "log" -> log10(x)
                    "ln" -> ln(x)
                    "sqrt" -> sqrt(x)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            }
        } else {
            throw RuntimeException("Unexpected: " + ch.toChar())
        }

        if (eat('^'.toInt())) x = x.pow(parseFactor())
        if (eat('%'.toInt())) x = x / 100.0

        return x
    }
}

