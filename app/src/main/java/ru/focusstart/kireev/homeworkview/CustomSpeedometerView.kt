package ru.focusstart.kireev.homeworkview

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.properties.Delegates

class CustomSpeedometerView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attributeSet, defStyleAttr, defStyleRes) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var size = 550
    private var centerPoint by Delegates.notNull<Float>()
    private var outerEdgingBorderWidth by Delegates.notNull<Float>()
    private var bigCircleRadius by Delegates.notNull<Float>()
    private var innerCircleForSegmentsRadius by Delegates.notNull<Float>()
    private var thicknessLines by Delegates.notNull<Float>()
    private var arrowLength by Delegates.notNull<Float>()
    private var textSize by Delegates.notNull<Float>()
    private var textWidth by Delegates.notNull<Float>()
    private var bgColor = Color.BLACK
    private var animateDrive = AnimatorSet()
    private val animateStopDrive = AnimatorSet()

    private var arrowColor = Color.BLACK
        set(value) {
            field = value
        }
    private var segmentsColor = Color.parseColor("#B2FF59")
    private var outerEdgingColor = Color.GRAY

    private var currentSpeed by Delegates.notNull<Int>()


    private val degreesForSegments =
        listOf(150, 165, 180, 195, 210, 225, 240, 255, 270, 285, 300, 315, 330, 345, 360, 15, 30)
    private val textOfSpeedForSegments =
        listOf("0", "20", "40", "60", "80", "100", "120", "140", "160")
    private lateinit var pointsForTextSpeed: List<Degrees>
    private lateinit var pointsOfSegment: List<Pair<Degrees, Degrees>>

    init {
        val typedArray = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.CustomSpeedometerView,
            defStyleAttr,
            defStyleRes
        )

        try {
            bgColor =
                typedArray.getColor(R.styleable.CustomSpeedometerView_colorBackground, bgColor)
            segmentsColor =
                typedArray.getColor(R.styleable.CustomSpeedometerView_colorSegments, segmentsColor)
        } finally {
            typedArray.recycle()
        }
    }

    fun drive() {
        when (currentSpeed) {
            in 0..40 ->
                animateArrow(8000, 40, 35, 80, 75, 120, 115, 160)

            in 41..80 ->
                animateArrow(4000, 80, 75, 120, 115, 160)

            in 81..120 ->
                animateArrow(3000, 120, 115, 160)

            else ->
                animateArrow(2000, 160)

        }
    }

    private fun animateArrow(duration: Long, vararg speed: Int) {
        if (animateStopDrive.isRunning) {
            animateStopDrive.cancel()
        }
        val animArrow = ValueAnimator.ofInt(currentSpeed, *speed).apply {
            addUpdateListener {
                currentSpeed = this.animatedValue as Int
                invalidate()
            }
            this.duration = duration
        }

        val animArrowColor = ValueAnimator.ofInt(arrowColor, Color.RED).apply {
            setEvaluator(ArgbEvaluator())
            addUpdateListener {
                if (currentSpeed > 100) {
                    val color = this.animatedValue as Int
                    arrowColor = color
                }
            }
            this.duration = (duration * 1.3).toLong()
        }
        animateDrive = AnimatorSet()
        animateDrive.play(animArrow)
            .with(animArrowColor)
        animateDrive.start()
    }

    fun stopDrive() {
        if (animateDrive.isRunning) {
            animateDrive.cancel()
        }
        val animArrow = ValueAnimator.ofInt(currentSpeed, 0).apply {
            addUpdateListener {
                currentSpeed = this.animatedValue as Int
                invalidate()
            }
            this.duration = 4000
        }
        val animColor = ValueAnimator.ofInt(arrowColor, Color.BLACK).apply {
            duration = 2000
            setEvaluator(ArgbEvaluator())
            addUpdateListener {
                val color = this.animatedValue as Int
                arrowColor = color
            }
        }

        animateStopDrive.play(animArrow)
            .with(animColor)
        animateStopDrive.start()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureDimension(size, widthMeasureSpec)
        val height = measureDimension(size, heightMeasureSpec)

        size = min(width, height)
        initVariables()
        setMeasuredDimension(width, height)
    }

    private fun measureDimension(minSize: Int, measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        return when (specMode) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> minSize.coerceAtMost(specSize)
            else -> minSize
        }
    }

    private fun initVariables() {
        centerPoint = size / 2f
        outerEdgingBorderWidth = size * 0.03f
        bigCircleRadius = centerPoint - outerEdgingBorderWidth / 2
        innerCircleForSegmentsRadius = bigCircleRadius - size * 0.6f
        pointsOfSegment = calculateCoordinatesForSegments(
            bigCircleRadius - size * 0.01f,
            bigCircleRadius - size * 0.1f
        )
        thicknessLines = outerEdgingBorderWidth / 1.9f
        arrowLength = bigCircleRadius * 0.82f
        pointsForTextSpeed = calculateCoordinatesForText(bigCircleRadius - size * 0.18f)
        textSize = size * 0.06f
        textWidth = thicknessLines / 3
        shaderPaint.shader = createShader()
        currentSpeed = 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //внешний кант
        paint.color = outerEdgingColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outerEdgingBorderWidth
        canvas.drawCircle(centerPoint, centerPoint, centerPoint - outerEdgingBorderWidth / 2, paint)

        //внутренний круг с переходом цвета
        canvas.drawCircle(centerPoint, centerPoint, bigCircleRadius, shaderPaint)

        //сегменты циферблата
        paint.style = Paint.Style.FILL
        paint.strokeWidth = thicknessLines
        paint.color = segmentsColor
        for (segments in pointsOfSegment) {
            canvas.drawLine(
                segments.first.first,
                segments.first.second,
                segments.second.first,
                segments.second.second,
                paint
            )
        }

        //цифры
        paint.textSize = textSize
        paint.textAlign = Paint.Align.CENTER
        paint.strokeWidth = textWidth
        for ((index, textCoordinates) in pointsForTextSpeed.withIndex()) {
            canvas.drawText(
                textOfSpeedForSegments[index],
                textCoordinates.first,
                textCoordinates.second,
                paint
            )
        }

        //стрелка
        paint.color = arrowColor
        paint.strokeWidth = thicknessLines
        canvas.drawLine(
            centerPoint,
            centerPoint,
            calculatePoint(arrowLength, true, convertSpeedToDegrees(currentSpeed)),
            calculatePoint(arrowLength, false, convertSpeedToDegrees(currentSpeed)),
            paint
        )
    }

    private fun calculateCoordinatesForSegments(
        firstRadius: Float,
        secondRadius: Float
    ): List<Pair<Degrees, Degrees>> {
        val mutableList = mutableListOf<Pair<Degrees, Degrees>>()
        for (degree in degreesForSegments) {
            val startX = calculatePoint(firstRadius, true, degree)
            val startY = calculatePoint(firstRadius, false, degree)
            val endX = calculatePoint(secondRadius, true, degree)
            val endY = calculatePoint(secondRadius, false, degree)
            mutableList.add(Pair(Degrees(startX, startY), Degrees(endX, endY)))
        }
        return mutableList
    }

    private fun calculateCoordinatesForText(radius: Float): List<Degrees> {
        val mutableList = mutableListOf<Pair<Float, Float>>()
        for ((index, degree) in degreesForSegments.withIndex()) {
            if (index % 2 == 0) {
                val x = calculatePoint(radius, true, degree)
                val y = calculatePoint(radius, false, degree)
                mutableList.add(Degrees(x, y))
            }
        }
        return mutableList
    }

    private fun calculatePoint(radius: Float, isPointX: Boolean, degree: Int): Float {
        val point = if (isPointX) {
            cos(degree * Math.PI / 180f).toFloat()
        } else {
            sin(degree * Math.PI / 180f).toFloat()
        }
        return radius * point + centerPoint
    }

    private fun createShader(): Shader {
        val shader =
            RadialGradient(0f, 0f, 1f, Color.WHITE, bgColor, Shader.TileMode.CLAMP) as Shader
        val matrix = Matrix()
        matrix.postTranslate(centerPoint, centerPoint)
        matrix.postScale(centerPoint, centerPoint, centerPoint, centerPoint)
        shader.setLocalMatrix(matrix)
        return shader
    }

    private fun convertSpeedToDegrees(speed: Int): Int {
        var degree = (speed * 1.5 + 150).toInt()
        if (degree < 150) {
            degree = 150
        }
        if (degree > 360) {
            degree -= 360
        }
        return degree
    }
}

typealias Degrees = Pair<Float, Float>