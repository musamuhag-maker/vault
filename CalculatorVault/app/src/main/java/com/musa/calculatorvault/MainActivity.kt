package com.musa.calculatorvault

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.text.DecimalFormat
import java.util.concurrent.Executor

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("vault", MODE_PRIVATE) }
    private var expression = ""
    private lateinit var display: TextView
    private var secretTaps = 0
    private var tapResetAt = 0L
    private val blue = Color.rgb(75, 139, 245)
    private val dark = Color.rgb(32, 33, 36)
    private val light = Color.rgb(245, 245, 245)

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showCalculator() }

    private fun showCalculator() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 20, 18, 16); setBackgroundColor(light) }
        display = TextView(this).apply { text = "0"; textSize = 38f; gravity = Gravity.END or Gravity.CENTER_VERTICAL; setTextColor(dark); setPadding(8, 18, 8, 18); maxLines = 2 }
        root.addView(display, LinearLayout.LayoutParams(-1, 0, 1.2f))
        val rows = listOf(listOf("AC", "±", "%", "÷"), listOf("7", "8", "9", "×"), listOf("4", "5", "6", "−"), listOf("1", "2", "3", "+"), listOf("⌫", "0", ".", "="))
        rows.forEachIndexed { rowIndex, row ->
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.forEachIndexed { colIndex, label ->
                val b = TextView(this).apply {
                    text = label; textSize = 23f; gravity = Gravity.CENTER; setTextColor(if (label in listOf("÷", "×", "−", "+", "=")) Color.WHITE else dark)
                    setBackgroundColor(if (label in listOf("÷", "×", "−", "+", "=")) blue else if (label in listOf("AC", "±", "%", "⌫")) Color.rgb(225, 228, 232) else Color.WHITE)
                    setOnClickListener { onKey(label) }
                    setOnLongClickListener { if (label == "=") { openVaultGate(); true } else false }
                }
                val lp = LinearLayout.LayoutParams(0, 0, 1f).apply { height = dp(64); setMargins(dp(4), dp(4), dp(4), dp(4)) }
                line.addView(b, lp)
            }
            root.addView(line, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        val hint = TextView(this).apply { text = "Calculator"; textSize = 12f; gravity = Gravity.CENTER; setTextColor(Color.GRAY); setPadding(0, 8, 0, 0) }
        hint.setOnClickListener {
            val now = System.currentTimeMillis(); if (now - tapResetAt > 1800) secretTaps = 0
            tapResetAt = now; secretTaps++
            if (secretTaps >= 7) { secretTaps = 0; openVaultGate() }
        }
        root.addView(hint)
        setContentView(root)
    }

    private fun onKey(k: String) {
        when (k) {
            "AC" -> expression = ""
            "⌫" -> expression = expression.dropLast(1)
            "±" -> if (expression.isNotEmpty()) expression = if (expression.startsWith("-")) expression.drop(1) else "-$expression"
            "%" -> { val v = expression.toDoubleOrNull(); if (v != null) expression = (v / 100.0).toString() }
            "=" -> { val result = evaluate(expression); if (result != null) expression = DecimalFormat("0.##########").format(result).replace(',', '.') }
            else -> expression += when (k) { "÷" -> "/"; "×" -> "*"; "−" -> "-"; else -> k }
        }
        display.text = expression.ifEmpty { "0" }
    }

    private fun evaluate(raw: String): Double? = try {
        val s = raw.replace(" ", "")
        if (s.isEmpty() || !s.matches(Regex("[0-9.+*/()\\-]+"))) return null
        val tokens = Regex("(?:\\d+(?:\\.\\d*)?|\\.\\d+|[()+*/-])").findAll(s).map { it.value }.toList()
        if (tokens.joinToString("") != s) return null
        Parser(tokens).parse()
    } catch (_: Exception) { null }

    private class Parser(private val t: List<String>) {
        private var p = 0
        fun parse(): Double { val x = add(); if (p != t.size) throw IllegalArgumentException(); return x }
        private fun add(): Double { var x = mul(); while (p < t.size && (t[p] == "+" || t[p] == "-")) { val op=t[p++]; val y=mul(); x=if(op=="+") x+y else x-y }; return x }
        private fun mul(): Double { var x = unary(); while (p < t.size && (t[p] == "*" || t[p] == "/")) { val op=t[p++]; val y=unary(); x=if(op=="*") x*y else x/y }; return x }
        private fun unary(): Double { if (p<t.size && t[p]=="-") { p++; return -unary() }; if (p<t.size && t[p]=="+") { p++; return unary() }; if (p<t.size && t[p]=="(") { p++; val x=add(); if(p>=t.size || t[p++]!=")") throw IllegalArgumentException(); return x }; if(p>=t.size) throw IllegalArgumentException(); return t[p++].toDouble() }
    }

    private fun openVaultGate() {
        if (Build.VERSION.SDK_INT >= 28) {
            val executor = Executor { it.run() }
            val prompt = BiometricPrompt.Builder(this).setTitle("Private app area").setSubtitle("Verify your fingerprint to continue").setNegativeButton("Cancel", executor) { _, _ -> }.build()
            prompt.authenticate(CancellationSignal(), executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) { runOnUiThread { showVault() } }
                override fun onAuthenticationFailed() { super.onAuthenticationFailed() }
            })
        } else { Toast.makeText(this, "Fingerprint unlock needs Android 9 or newer", Toast.LENGTH_LONG).show() }
    }

    private data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)
    private fun installedApps(): List<AppEntry> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA).filter { it.packageName != packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppEntry(pm.getApplicationLabel(it).toString(), it.packageName, pm.getApplicationIcon(it)) }.sortedBy { it.label.lowercase() }
    }
    private fun showVault() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(20),dp(16),dp(12)); setBackgroundColor(light) }
        val title = TextView(this).apply { text="Private app area"; textSize=24f; setTextColor(dark); typeface=Typeface.DEFAULT_BOLD }
        root.addView(title)
        val note = TextView(this).apply { text="Choose apps to hide from this launcher's app list. Set Calculator as your default Home app for this to work."; textSize=13f; setTextColor(Color.DKGRAY); setPadding(0,dp(8),0,dp(12)) }
        root.addView(note)
        val scroll = ScrollView(this); val list = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; scroll.addView(list); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        val hidden = prefs.getStringSet("hidden", emptySet())?.toMutableSet() ?: mutableSetOf()
        installedApps().forEach { app ->
            val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(0,dp(7),0,dp(7)) }
            val icon = ImageView(this).apply { setImageDrawable(app.icon) }; row.addView(icon, LinearLayout.LayoutParams(dp(40),dp(40)))
            val name = TextView(this).apply { text=app.label; textSize=16f; setTextColor(dark); setPadding(dp(10),0,0,0) }; row.addView(name, LinearLayout.LayoutParams(0,dp(48),1f))
            val check = CheckBox(this).apply { isChecked=hidden.contains(app.packageName); setOnCheckedChangeListener { _, checked -> if(checked) hidden.add(app.packageName) else hidden.remove(app.packageName); prefs.edit().putStringSet("hidden", hidden.toSet()).apply() } }
            row.addView(check); list.addView(row)
            val divider = View(this).apply { setBackgroundColor(0xFFE0E0E0.toInt()) }; list.addView(divider, LinearLayout.LayoutParams(-1,dp(1)))
        }
        val buttons = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.END }
        val done = Button(this).apply { text="Done"; setOnClickListener { showCalculator() } }; buttons.addView(done); root.addView(buttons)
        setContentView(root)
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
