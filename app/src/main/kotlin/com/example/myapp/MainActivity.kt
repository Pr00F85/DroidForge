package com.example.myapp

import android.animation.*
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        val ICON_SETS  = listOf(Pair("X","O"), Pair("*","@"), Pair("+","-"), Pair("^","~"))
        val ICON_NAMES = listOf("Classic X/O", "Star/Ring", "Plus/Minus", "Up/Wave")
        val P1_COLOR = Color.parseColor("#E74C3C")
        val P2_COLOR = Color.parseColor("#3498DB")
        val BG       = Color.parseColor("#1A1B2E")
        val CELL_BG  = Color.parseColor("#16213E")
        val PANEL_BG = Color.parseColor("#0F3460")
    }

    private val board = Array(3){ IntArray(3) }
    private var current     = 1
    private var gameActive  = false
    private var twoPlayer   = true
    private var iconIdx     = 0
    private val scores      = intArrayOf(0,0,0)

    private val cells       = Array(3){ arrayOfNulls<TextView>(3) }
    private lateinit var statusTv   : TextView
    private lateinit var sc1Tv      : TextView
    private lateinit var sc2Tv      : TextView
    private lateinit var drawTv     : TextView
    private lateinit var modeBtn    : Button
    private lateinit var iconBtn    : Button
    private lateinit var board_grid : GridLayout
    private lateinit var overlay    : FrameLayout
    private lateinit var overlayTv  : TextView

    private val tone    = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.statusBarColor     = BG
        window.navigationBarColor = BG
        buildUI()
        newGame()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ── UI BUILD ────────────────────────────────────────────────────────────
    private fun buildUI() {
        val root = FrameLayout(this).apply { setBackgroundColor(BG) }
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false }
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(28), dp(16), dp(24))
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Title
        main.addView(TextView(this).apply {
            text = "TIC TAC TOE"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            letterSpacing = 0.15f
        }, lp(ww = true, h = WRAP).also { (it as LinearLayout.LayoutParams).bottomMargin = dp(16) })

        // Scoreboard
        val scoreRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(PANEL_BG)
            setPadding(dp(8), dp(12), dp(8), dp(12))
        }
        val col1 = scoreCol("PLAYER 1", P1_COLOR); sc1Tv = col1.second
        val col2 = scoreCol("DRAWS",    Color.GRAY); drawTv= col2.second
        val col3 = scoreCol("PLAYER 2", P2_COLOR); sc2Tv = col3.second
        scoreRow.addView(col1.first, LinearLayout.LayoutParams(0, WRAP, 1f))
        scoreRow.addView(col2.first, LinearLayout.LayoutParams(0, WRAP, 1f))
        scoreRow.addView(col3.first, LinearLayout.LayoutParams(0, WRAP, 1f))
        main.addView(scoreRow, lp(ww = true, h = WRAP).also { (it as LinearLayout.LayoutParams).bottomMargin = dp(16) })

        // Status
        statusTv = TextView(this).apply {
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(10))
        }
        main.addView(statusTv, lp(ww = true, h = WRAP))

        // Board
        val dm = resources.displayMetrics.widthPixels - dp(64)
        val cs = dm / 3
        board_grid = GridLayout(this).apply { rowCount = 3; columnCount = 3 }

        for (r in 0..2) for (c in 0..2) {
            val tv = TextView(this).apply {
                textSize = 44f
                gravity = Gravity.CENTER
                setBackgroundColor(CELL_BG)
                setTextColor(Color.WHITE)
                setOnClickListener { onCell(r, c) }
            }
            val gp = GridLayout.LayoutParams().apply {
                width = cs; height = cs
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            board_grid.addView(tv, gp)
            cells[r][c] = tv
        }
        val bs = dm + dp(18)
        main.addView(board_grid, LinearLayout.LayoutParams(bs, bs).also {
            it.gravity = Gravity.CENTER_HORIZONTAL
            it.bottomMargin = dp(20)
        })

        // Buttons row 1
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        modeBtn = mkBtn("2P Mode") {
            twoPlayer = !twoPlayer
            modeBtn.text = if (twoPlayer) "2P Mode" else "vs AI"
            newGame()
        }
        iconBtn = mkBtn("Icons: Classic") {
            iconIdx = (iconIdx + 1) % ICON_SETS.size
            iconBtn.text = "Icons: " + ICON_NAMES[iconIdx]
            newGame()
        }
        row1.addView(modeBtn, LinearLayout.LayoutParams(0, WRAP, 1f).also { it.marginEnd = dp(8) })
        row1.addView(iconBtn, LinearLayout.LayoutParams(0, WRAP, 1f))
        main.addView(row1, lp(ww = true, h = WRAP).also { (it as LinearLayout.LayoutParams).bottomMargin = dp(8) })

        // New Game button
        main.addView(mkBtn("NEW GAME", Color.parseColor("#27AE60")) { newGame() },
            lp(ww = true, h = WRAP))

        scroll.addView(main)
        root.addView(scroll)

        // Overlay
        overlay = FrameLayout(this).apply {
            visibility = View.GONE
            setBackgroundColor(Color.parseColor("#CC000000"))
        }
        overlayTv = TextView(this).apply {
            textSize = 36f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        overlay.addView(overlayTv, FrameLayout.LayoutParams(MATCH, MATCH).also { it.gravity = Gravity.CENTER })
        overlay.setOnClickListener { overlay.visibility = View.GONE; newGame() }
        root.addView(overlay)

        setContentView(root)
    }

    private val WRAP  = ViewGroup.LayoutParams.WRAP_CONTENT
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

    private fun lp(ww: Boolean = false, h: Int = WRAP): ViewGroup.LayoutParams =
        LinearLayout.LayoutParams(if (ww) MATCH else WRAP, h)

    private fun scoreCol(label: String, color: Int): Pair<LinearLayout, TextView> {
        val numTv = TextView(this).apply {
            text = "0"; textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(color); gravity = Gravity.CENTER
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            addView(TextView(context).apply {
                text = label; textSize = 10f
                setTextColor(Color.LTGRAY); gravity = Gravity.CENTER
            })
            addView(numTv)
        }
        return Pair(col, numTv)
    }

    private fun mkBtn(txt: String, bg: Int = PANEL_BG, fn: () -> Unit): Button =
        Button(this).apply {
            text = txt; isAllCaps = false; textSize = 13f
            setBackgroundColor(bg); setTextColor(Color.WHITE)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { fn() }
        }

    // ── GAME LOGIC ──────────────────────────────────────────────────────────
    private fun newGame() {
        for (r in 0..2) for (c in 0..2) {
            board[r][c] = 0
            cells[r][c]?.apply {
                text = ""; alpha = 1f; scaleX = 1f; scaleY = 1f
                setBackgroundColor(CELL_BG); isEnabled = true
            }
        }
        current = 1; gameActive = true
        updateStatus()
    }

    private fun onCell(r: Int, c: Int) {
        if (!gameActive || board[r][c] != 0) return
        place(r, c)
    }

    private fun place(r: Int, c: Int) {
        board[r][c] = current
        val icons = ICON_SETS[iconIdx]
        val sym   = if (current == 1) icons.first else icons.second
        val col   = if (current == 1) P1_COLOR    else P2_COLOR
        cells[r][c]?.apply {
            text = sym; setTextColor(col)
            scaleX = 0f; scaleY = 0f
            animate().scaleX(1f).scaleY(1f).setDuration(200)
                .setInterpolator(OvershootInterpolator(2f)).start()
        }
        playPlace()

        val win = winner()
        when {
            win != null -> {
                gameActive = false
                if (current == 1) scores[0]++ else scores[2]++
                updateScores()
                animWin(win)
                val name = if (current == 1) "Player 1" else if (twoPlayer) "Player 2" else "AI"
                handler.postDelayed({ showOverlay("WINNER
" + name + "!") }, 700)
            }
            isDraw() -> {
                gameActive = false; scores[1]++; updateScores()
                animDraw()
                handler.postDelayed({ showOverlay("DRAW!") }, 700)
            }
            else -> {
                current = if (current == 1) 2 else 1
                updateStatus()
                if (!twoPlayer && current == 2) handler.postDelayed({ aiMove() }, 450)
            }
        }
    }

    private fun aiMove() {
        if (!gameActive) return
        bestMove()?.let { place(it.first, it.second) }
    }

    private fun bestMove(): Pair<Int,Int>? {
        for (r in 0..2) for (c in 0..2) if (board[r][c]==0) {
            board[r][c]=2; val w=winner()!=null; board[r][c]=0
            if (w) return Pair(r,c)
        }
        for (r in 0..2) for (c in 0..2) if (board[r][c]==0) {
            board[r][c]=1; val w=winner()!=null; board[r][c]=0
            if (w) return Pair(r,c)
        }
        if (board[1][1]==0) return Pair(1,1)
        val corners = listOf(Pair(0,0),Pair(0,2),Pair(2,0),Pair(2,2)).filter { board[it.first][it.second]==0 }
        if (corners.isNotEmpty()) return corners.random()
        val empty = mutableListOf<Pair<Int,Int>>()
        for (r in 0..2) for (c in 0..2) if (board[r][c]==0) empty.add(Pair(r,c))
        return empty.randomOrNull()
    }

    private fun winner(): List<Pair<Int,Int>>? {
        val lines = listOf(
            listOf(Pair(0,0),Pair(0,1),Pair(0,2)),
            listOf(Pair(1,0),Pair(1,1),Pair(1,2)),
            listOf(Pair(2,0),Pair(2,1),Pair(2,2)),
            listOf(Pair(0,0),Pair(1,0),Pair(2,0)),
            listOf(Pair(0,1),Pair(1,1),Pair(2,1)),
            listOf(Pair(0,2),Pair(1,2),Pair(2,2)),
            listOf(Pair(0,0),Pair(1,1),Pair(2,2)),
            listOf(Pair(0,2),Pair(1,1),Pair(2,0))
        )
        for (l in lines) {
            val v = l.map { board[it.first][it.second] }
            if (v.all{it==1} || v.all{it==2}) return l
        }
        return null
    }

    private fun isDraw() = board.all { r -> r.all { it != 0 } }

    // ── ANIMATIONS ──────────────────────────────────────────────────────────
    private fun animWin(line: List<Pair<Int,Int>>) {
        for (r in 0..2) for (c in 0..2)
            if (!line.contains(Pair(r,c))) cells[r][c]?.animate()?.alpha(0.2f)?.setDuration(350)?.start()
        for (p in line) {
            val tv = cells[p.first][p.second] ?: continue
            val sx = ObjectAnimator.ofFloat(tv,"scaleX",1f,1.35f,1f).apply{ duration=350; repeatCount=4 }
            val sy = ObjectAnimator.ofFloat(tv,"scaleY",1f,1.35f,1f).apply{ duration=350; repeatCount=4 }
            AnimatorSet().apply{ playTogether(sx,sy); start() }
        }
        playWin()
    }

    private fun animDraw() {
        val shake = TranslateAnimation(-14f,14f,0f,0f).apply{
            duration=55; repeatCount=6; repeatMode=Animation.REVERSE
        }
        board_grid.startAnimation(shake)
        playDraw()
    }

    private fun showOverlay(msg: String) {
        overlayTv.text = msg + "

Tap to play again"
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.animate().alpha(1f).setDuration(300).start()
    }

    // ── SOUND ────────────────────────────────────────────────────────────────
    private fun playPlace() { try { tone.startTone(if(current==1) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_BEEP2, 70) } catch(e:Exception){} }
    private fun playWin()   { try { tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500) } catch(e:Exception){} }
    private fun playDraw()  { try { tone.startTone(ToneGenerator.TONE_PROP_NACK, 300) } catch(e:Exception){} }

    // ── STATUS ───────────────────────────────────────────────────────────────
    private fun updateStatus() {
        val icons = ICON_SETS[iconIdx]
        val sym  = if (current==1) icons.first else icons.second
        val name = if (current==1) "Player 1" else if (twoPlayer) "Player 2" else "AI"
        statusTv.text = name + " (" + sym + ") Turn"
        statusTv.setTextColor(if (current==1) P1_COLOR else P2_COLOR)
    }

    private fun updateScores() {
        sc1Tv.text  = scores[0].toString()
        drawTv.text = scores[1].toString()
        sc2Tv.text  = scores[2].toString()
    }

    override fun onDestroy() { super.onDestroy(); try { tone.release() } catch(e:Exception){} }
}