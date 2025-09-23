package com.example.androidtictactoe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class BoardView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var cellSize = 0f
    private var size = 0
    private val gridPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 10f
        style = Paint.Style.STROKE
    }

    // Asegúrate de tener los archivos ic_x.png y ic_o.png en res/drawable
    private val xBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.equis)
    private val oBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.circulo)

    private var board = Array(3) { Array(3) { "" } }
    var onBoardTouchListener: ((row: Int, col: Int) -> Unit)? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        size = min(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        cellSize = width / 3f

        drawGrid(canvas)
        drawPieces(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (it.action == MotionEvent.ACTION_DOWN) {
                val row = (it.y / cellSize).toInt()
                val col = (it.x / cellSize).toInt()
                if (row in 0..2 && col in 0..2) {
                    onBoardTouchListener?.invoke(row, col)
                }
                return true
            }
        }
        return false
    }

    private fun drawGrid(canvas: Canvas) {
        for (i in 1..2) {
            // Líneas verticales
            canvas.drawLine(i * cellSize, 0f, i * cellSize, height.toFloat(), gridPaint)
            // Líneas horizontales
            canvas.drawLine(0f, i * cellSize, width.toFloat(), i * cellSize, gridPaint)
        }
    }

    private fun drawPieces(canvas: Canvas) {
        for (row in 0..2) {
            for (col in 0..2) {
                val piece = board[row][col]
                if (piece.isNotEmpty()) {
                    val bitmap = if (piece == "X") xBitmap else oBitmap
                    canvas.drawBitmap(
                        bitmap,
                        null,
                        RectF(
                            col * cellSize,
                            row * cellSize,
                            (col + 1) * cellSize,
                            (row + 1) * cellSize
                        ),
                        null
                    )
                }
            }
        }
    }

    fun setBoard(newBoard: Array<Array<String>>) {
        board = newBoard
        invalidate() // Le dice a la vista que se redibuje
    }
}