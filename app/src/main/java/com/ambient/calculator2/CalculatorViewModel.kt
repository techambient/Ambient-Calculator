package com.ambient.calculator2

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("calculator_prefs", Context.MODE_PRIVATE)

    var display by mutableStateOf("")
        private set

    var result by mutableStateOf("")
        private set

    var livePreview by mutableStateOf("")
        private set

    var isAdvancedMode by mutableStateOf(prefs.getBoolean("advanced_mode", false))
        private set

    var isBusinessMode by mutableStateOf(prefs.getBoolean("business_mode", false))
        private set

    var isAmoledMode by mutableStateOf(prefs.getBoolean("amoled_mode", false))
        private set

    var isIncognitoMode by mutableStateOf(prefs.getBoolean("incognito_mode", false))
        private set

    var isHapticEnabled by mutableStateOf(prefs.getBoolean("haptic_enabled", false))
        private set

    var isAutoCopyEnabled by mutableStateOf(prefs.getBoolean("auto_copy_enabled", true))
        private set

    var isKeepAwakeEnabled by mutableStateOf(prefs.getBoolean("keep_awake_enabled", false))
        private set

    var isHandwritingEnabled by mutableStateOf(prefs.getBoolean("handwriting_enabled", false))
        private set

    var history by mutableStateOf(listOf<HistoryItem>())
        private set

    private var previewJob: Job? = null

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Number -> enterNumber(action.number)
            is CalculatorAction.Operator -> enterOperator(action.operator)
            is CalculatorAction.Clear -> clear()
            is CalculatorAction.ClearEntry -> clearEntry()
            is CalculatorAction.Backspace -> backspace()
            is CalculatorAction.Decimal -> enterDecimal()
            is CalculatorAction.Calculate -> calculate()
            is CalculatorAction.ToggleMode -> {
                isAdvancedMode = !isAdvancedMode
                prefs.edit().putBoolean("advanced_mode", isAdvancedMode).apply()
            }
            is CalculatorAction.ToggleBusinessMode -> {
                isBusinessMode = !isBusinessMode
                prefs.edit().putBoolean("business_mode", isBusinessMode).apply()
            }
            is CalculatorAction.ToggleAmoledMode -> {
                isAmoledMode = !isAmoledMode
                prefs.edit().putBoolean("amoled_mode", isAmoledMode).apply()
            }
            is CalculatorAction.ToggleIncognitoMode -> {
                isIncognitoMode = !isIncognitoMode
                prefs.edit().putBoolean("incognito_mode", isIncognitoMode).apply()
            }
            is CalculatorAction.ToggleHaptic -> {
                isHapticEnabled = !isHapticEnabled
                prefs.edit().putBoolean("haptic_enabled", isHapticEnabled).apply()
            }
            is CalculatorAction.ToggleAutoCopy -> {
                isAutoCopyEnabled = !isAutoCopyEnabled
                prefs.edit().putBoolean("auto_copy_enabled", isAutoCopyEnabled).apply()
            }
            is CalculatorAction.ToggleKeepAwake -> {
                isKeepAwakeEnabled = !isKeepAwakeEnabled
                prefs.edit().putBoolean("keep_awake_enabled", isKeepAwakeEnabled).apply()
            }
            is CalculatorAction.ToggleHandwriting -> {
                isHandwritingEnabled = !isHandwritingEnabled
                prefs.edit().putBoolean("handwriting_enabled", isHandwritingEnabled).apply()
            }
            is CalculatorAction.Function -> enterFunction(action.function)
            is CalculatorAction.Constant -> enterConstant(action.constant)
            is CalculatorAction.Sign -> changeSign()
            is CalculatorAction.ClearHistory -> history = emptyList()
            is CalculatorAction.SelectHistory -> {
                display = action.item.expression
                result = action.item.result
                updatePreview(immediate = true)
            }
            is CalculatorAction.InsertScannedText -> {
                // Map common handwriting/OCR symbols to calculator operators
                val mappedText = action.text
                    .replace("×", "*")
                    .replace("x", "*")
                    .replace("X", "*")
                    .replace("÷", "/")
                    .replace(":", "/")
                    .replace("−", "-")
                    .replace(",", ".")
                    .replace(" ", "")
                
                val cleaned = mappedText.filter { 
                    it.isDigit() || isOperator(it) || it == '(' || it == ')' || it == '.' || it == 'π' || it == 'e' 
                }

                if (cleaned.isNotEmpty()) {
                    // Append if current display is not a static result
                    if (result.isNotEmpty() && display == result) {
                        display = cleaned
                        result = ""
                    } else {
                        display += cleaned
                    }
                }
                updatePreview(immediate = true)
            }
        }
        if (action !is CalculatorAction.Calculate && action !is CalculatorAction.SelectHistory && action !is CalculatorAction.InsertScannedText) {
            updatePreview(immediate = false)
        }
    }

    private fun updatePreview(immediate: Boolean) {
        previewJob?.cancel()
        if (display.isEmpty()) {
            livePreview = ""
            return
        }
        
        previewJob = viewModelScope.launch(Dispatchers.Default) {
            if (!immediate) {
                delay(300) // Increased debounce to 300ms to eliminate typing lag and hangs
            }
            try {
                val evalResult = ExpressionEvaluator(display, isBusinessMode).evaluate()
                val formatted = formatResult(evalResult)
                launch(Dispatchers.Main) {
                    livePreview = formatted
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    livePreview = ""
                }
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
            val evalResult = ExpressionEvaluator(display, isBusinessMode).evaluate()
            val formattedResult = formatResult(evalResult)
            
            // Add to history only if not in incognito mode
            if (!isIncognitoMode) {
                history = listOf(HistoryItem(display, formattedResult)) + history
            }
            
            result = formattedResult
            display = formattedResult 
            livePreview = "" 
        } catch (e: Exception) {
            result = "Error"
            livePreview = ""
        }
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        val longVal = value.toLong()
        return if (value == longVal.toDouble()) {
            longVal.toString()
        } else {
            val formatted = "%.10f".format(value).trimEnd('0').trimEnd('.')
            if (formatted == "-0") "0" else formatted
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
    data class InsertScannedText(val text: String) : CalculatorAction()
    object ToggleBusinessMode : CalculatorAction()
    object ToggleAmoledMode : CalculatorAction()
    object ToggleIncognitoMode : CalculatorAction()
    object ToggleHaptic : CalculatorAction()
    object ToggleAutoCopy : CalculatorAction()
    object ToggleKeepAwake : CalculatorAction()
    object ToggleHandwriting : CalculatorAction()
}

class ExpressionEvaluator(private val expression: String, private val isBusinessMode: Boolean = false) {
    private var pos = -1
    private var ch = 0
    private var lastWasPercent = false

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
            if (eat('+'.toInt())) {
                lastWasPercent = false
                val y = parseTerm()
                if (isBusinessMode && lastWasPercent) {
                    x += x * y 
                } else {
                    x += y
                }
            } else if (eat('-'.toInt())) {
                lastWasPercent = false
                val y = parseTerm()
                if (isBusinessMode && lastWasPercent) {
                    x -= x * y
                } else {
                    x -= y
                }
            } else return x
        }
    }

    private fun parseTerm(): Double {
        var x = parseFactor()
        while (true) {
            if (eat('*'.toInt())) {
                lastWasPercent = false
                x *= parseFactor()
            } else if (eat('/'.toInt())) {
                lastWasPercent = false
                x /= parseFactor()
            } else return x
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
        if (eat('%'.toInt())) {
            x = x / 100.0
            lastWasPercent = true
        }

        return x
    }
}
