package com.hellowwwwp.swipe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.*
import android.view.View.OnTouchListener
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import kotlin.math.*

/**
 * @author: wangpan
 * @email: p.wang0813@gmail.com
 * @date: 2022/11/10
 */
@SuppressLint("ClickableViewAccessibility")
class SwipeMenuLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    FrameLayout(context, attrs, defStyleAttr) {

    private val scroller: OverScroller = OverScroller(context)
    private val touchSlop: Int
    private val minimumVelocity: Int
    private val maximumVelocity: Int
    private var velocityTracker: VelocityTracker? = null

    private val openLimit: Float = 0.25f
    private val baseSettleDuration: Int = 250 // ms
    private val maxSettleDuration: Int = 350 // ms

    private var downMotionX: Int = 0
    private var downMotionY: Int = 0
    private var lastMotionX: Int = 0
    private var lastMotionY: Int = 0
    private var isBeingDragged: Boolean = false

    private var lastScrollX: Int = 0

    private var isDragStart: Boolean = false

    private var isMenuOpen: Boolean = false

    private val listeners: MutableList<OnSwipeChangedListener> = mutableListOf()

    private var contentView: View? = null

    private var leftMenuView: View? = null

    private var rightMenuView: View? = null

    private var dragMenu: View? = null

    private val rect: Rect = Rect()

    private var _isSwipeEnabled: Boolean = true

    var isSwipeEnabled: Boolean
        set(value) {
            _isSwipeEnabled = value
            if (!value) {
                close(smooth = false)
            }
        }
        get() = _isSwipeEnabled

    /**
     * 是否启用严格拦截事件模式, 默认不启用
     */
    var isStrictInterceptEnabled: Boolean = false

    /**
     * 是否开启自动关闭功能, 默认 true
     * 处理在非 close 状态下点击 content 时, 自动 close 操作
     */
    var autoCloseEnabled: Boolean = true

    private var scrollContentToEndLeftRunnable: Runnable? = null

    private val View.isMenuEnabled: Boolean
        get() = this.visibility == View.VISIBLE

    private val View.dragEdge: DragEdge
        get() {
            val params = this.layoutParams as SwipeLayoutParams
            return params.dragEdge
        }

    private val View.showMode: ShowMode
        get() {
            val params = this.layoutParams as SwipeLayoutParams
            return params.showMode
        }

    private val View.dragPadding: Int
        get() {
            val params = this.layoutParams as SwipeLayoutParams
            return params.dragPadding
        }

    /**
     * 记录当前触摸在 layout 上的手指个数
     * 用于实现在一轮触摸事件中, 只能拖拽一个 menu 的功能 [resetDragMenuIfNeed]
     */
    private var touchPointCount: Int = 0

    private val onTouchListener = OnTouchListener { _, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                touchPointCount++
            }
            MotionEvent.ACTION_MOVE -> {
                if (touchPointCount == 0) {
                    touchPointCount++
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_CANCEL -> {
                touchPointCount--
            }
        }
        return@OnTouchListener false
    }

    val openEdge: DragEdge?
        get() {
            val content = this.contentView
            val menu = this.dragMenu
            if (content != null && menu != null && isOpenPosition(content, menu)) {
                return menu.dragEdge
            }
            return null
        }

    val isOpen: Boolean
        get() = openEdge != null

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SwipeMenuLayout)
        _isSwipeEnabled = ta.getBoolean(R.styleable.SwipeMenuLayout_sml_swipeEnabled, true)
        isStrictInterceptEnabled = ta.getBoolean(R.styleable.SwipeMenuLayout_sml_strictInterceptEnabled, false)
        autoCloseEnabled = ta.getBoolean(R.styleable.SwipeMenuLayout_sml_autoCloseEnabled, true)
        ta.recycle()

        val configuration = ViewConfiguration.get(context)
        touchSlop = configuration.scaledTouchSlop
        minimumVelocity = configuration.scaledMinimumFlingVelocity
        maximumVelocity = configuration.scaledMaximumFlingVelocity

        setOnTouchListener(onTouchListener)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (!_isSwipeEnabled) {
            return false
        }
        val action = event.action
        if (action == MotionEvent.ACTION_MOVE && isBeingDragged) {
            return true
        }
        val x = event.x.toInt()
        val y = event.y.toInt()
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                downMotionX = x
                downMotionY = y
                lastMotionX = x
                lastMotionY = y
                initOrResetVelocityTracker()
                velocityTracker?.addMovement(event)

                scroller.computeScrollOffset()
                isBeingDragged = !scroller.isFinished
            }
            MotionEvent.ACTION_MOVE -> {
                val xDiff = abs(x - lastMotionX)
                val yDiff = abs(y - lastMotionY)
                if (shouldInterceptTouchEvent(xDiff, yDiff)) {
                    isBeingDragged = true
                    lastMotionX = x
                    lastMotionY = y
                    initVelocityTrackerIfNotExists()
                    velocityTracker?.addMovement(event)
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                endDrag()
            }
        }
        return isBeingDragged || closeWhenTouchContentUp(event)
    }

    /**
     * 处理在非 close 状态下点击 content 时, 自动 close 操作
     */
    private fun closeWhenTouchContentUp(event: MotionEvent): Boolean {
        if (!autoCloseEnabled) {
            return false
        }
        val content = this.contentView
        val menu = this.dragMenu
        if (content == null || menu == null) {
            return false
        }
        //只在手指抬起时处理
        if (event.action != MotionEvent.ACTION_UP) {
            return false
        }
        val xDiff = abs(event.x - downMotionX)
        val yDiff = abs(event.y - downMotionY)
        //判断按下和抬起中间是否有产生滑动
        if (xDiff >= touchSlop || yDiff >= touchSlop) {
            return false
        }
        //判断当前已经是 close 的状态
        if (isClosePosition(content, menu)) {
            return false
        }
        //判断是否触摸在 content 的区域上
        if (isTouchInContent(event)) {
            close(null, true)
            return true
        }
        return false
    }

    private fun shouldInterceptTouchEvent(dx: Int, dy: Int): Boolean {
        if (!isStrictInterceptEnabled) {
            return abs(dx) >= touchSlop
        }
        return abs(dx) >= touchSlop && abs(dx) > abs(dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val consumed = super.onTouchEvent(event)
        if (!_isSwipeEnabled) {
            return consumed
        }
        initVelocityTrackerIfNotExists()
        val x = event.x.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isBeingDragged = !scroller.isFinished
                if (isBeingDragged) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                lastMotionX = x
            }
            MotionEvent.ACTION_MOVE -> {
                var deltaX = x - lastMotionX
                if (!isBeingDragged && abs(deltaX) > touchSlop) {
                    isBeingDragged = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    if (deltaX > 0) {
                        deltaX -= touchSlop
                    } else {
                        deltaX += touchSlop
                    }
                }
                if (isBeingDragged) {
                    lastMotionX = x
                    if (deltaX != 0) {
                        ensureDragMenuByDx(deltaX)
                        scrollContentBy(deltaX)
                    }
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                val xVelocity = velocityTracker?.xVelocity?.toInt() ?: 0
                val content = this.contentView
                val menu = this.dragMenu
                if (content != null && menu != null) {
                    if (shouldOpenMenu(content, menu, xVelocity)) {
                        open(menu.dragEdge, true, xVelocity)
                    } else {
                        close(menu.dragEdge, true, xVelocity)
                    }
                    resetDragMenuIfNeed()
                }
                endDrag()
            }
        }
        velocityTracker?.addMovement(event)
        return true
    }

    /**
     * 通过滑动的 dx 来确定要拖拽的 menu
     */
    private fun ensureDragMenuByDx(dx: Int): View? {
        return dragMenu ?: kotlin.run {
            if (dx > 0) {
                leftMenuView
            } else {
                rightMenuView
            }
        }.also {
            dragMenu = it
        }
    }

    /**
     * 通过 dragEdge 来确定要拖拽的 menu
     */
    private fun ensureDragMenuByDragEdge(dragEdge: DragEdge): View? {
        return when (dragEdge) {
            DragEdge.Left -> leftMenuView
            DragEdge.Right -> rightMenuView
        }.also {
            dragMenu = it
        }
    }

    /**
     * 重置 dragMenu, 避免在一次触摸过程可以拖拽两个 menu
     */
    private fun resetDragMenuIfNeed(): View? {
        val content = this.contentView
        val menu = this.dragMenu
        if (content == null || menu == null) {
            return null
        }
        return if (isClosePosition(content, menu) && touchPointCount == 0) {
            dragMenu = null
            return menu
        } else {
            null
        }
    }

    private fun scrollContentBy(dx: Int): Int {
        //移除正在等待被执行的滑动操作
        removeScrollContentToEndLeftRunnableIfNeed()

        val content = this.contentView
        val menu = this.dragMenu
        if (content == null || menu == null || !menu.isMenuEnabled) {
            return dx
        }

        val contentScrollRange = getContentScrollRange(menu)
        val minLeft: Int
        val maxLeft: Int
        when (menu.dragEdge) {
            DragEdge.Left -> {
                minLeft = paddingLeft
                maxLeft = paddingLeft + contentScrollRange
            }
            DragEdge.Right -> {
                minLeft = paddingLeft - contentScrollRange
                maxLeft = paddingLeft
            }
        }
        val oldLeft = content.left
        val newLeft = oldLeft + dx
        val consumed: Int = when {
            newLeft < minLeft -> minLeft - oldLeft
            newLeft > maxLeft -> maxLeft - oldLeft
            else -> dx
        }
        ViewCompat.offsetLeftAndRight(content, consumed)
        if (menu.showMode == ShowMode.PullOut && menu.isMenuEnabled) {
            layoutMenuWhenPullOut(menu)
        }
        val unconsumed = dx - consumed

        //回到状态改变开始
        notifyStateChangeStartIfNeed(menu, dx, consumed)
        //回调状态已经改变
        notifyStateChangedIfNeed(content, menu, consumed)
        //返回未消费的距离
        return unconsumed
    }

    private fun shouldOpenMenu(content: View, menu: View, xVelocity: Int): Boolean {
        val contentScrollRange = getContentScrollRange(menu)
        val scrollOpenLimit = contentScrollRange * openLimit
        when (menu.dragEdge) {
            DragEdge.Left -> {
                if (xVelocity >= minimumVelocity) {
                    return true
                }
                if (xVelocity > 0 && content.left >= scrollOpenLimit) {
                    return true
                }
                return false
            }
            DragEdge.Right -> {
                if (xVelocity < 0 && abs(xVelocity) >= minimumVelocity) {
                    return true
                }
                if (xVelocity < 0 && abs(content.left) >= scrollOpenLimit) {
                    return true
                }
                return false
            }
        }
    }

    private fun endDrag() {
        isBeingDragged = false
        recycleVelocityTracker()
    }

    private fun initOrResetVelocityTracker() {
        velocityTracker?.clear() ?: kotlin.run {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun initVelocityTrackerIfNotExists() {
        velocityTracker ?: kotlin.run {
            velocityTracker = VelocityTracker.obtain()
        }
    }

    private fun recycleVelocityTracker() {
        velocityTracker?.apply {
            recycle()
            velocityTracker = null
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            close(smooth = false)
        }
    }

    private fun getContentEndLeft(menu: View, open: Boolean): Int {
        if (open) {
            val contentScrollRange = getContentScrollRange(menu)
            return when (menu.dragEdge) {
                DragEdge.Left -> paddingLeft + contentScrollRange
                DragEdge.Right -> paddingLeft - contentScrollRange
            }
        }
        return paddingLeft
    }

    @JvmOverloads
    fun open(openEdge: DragEdge, smooth: Boolean = true): Boolean {
        return open(openEdge, smooth, 0)
    }

    private fun open(openEdge: DragEdge, smooth: Boolean = true, xVelocity: Int = 0): Boolean {
        return scrollContentToEndLeft(openEdge, true, smooth, xVelocity)
    }

    @JvmOverloads
    fun close(closeEdge: DragEdge? = null, smooth: Boolean = true): Boolean {
        return (closeEdge ?: dragMenu?.dragEdge)?.let { edge ->
            close(edge, smooth, 0)
        } ?: false
    }

    private fun close(dragEdge: DragEdge, smooth: Boolean = true, xVelocity: Int = 0): Boolean {
        return scrollContentToEndLeft(dragEdge, false, smooth, xVelocity)
    }

    private fun scrollContentToEndLeft(
        dragEdge: DragEdge,
        open: Boolean,
        smooth: Boolean,
        xVelocity: Int,
    ): Boolean {
        //还没完成布局, 先不要急着做滚动操作, 等布局完成再来滚动
        if (!isLaidOut) {
            //移除正在等待被执行的滑动操作
            removeScrollContentToEndLeftRunnableIfNeed()
            ScrollContentToEndLeftRunnable(this, dragEdge, open, smooth, xVelocity).let {
                scrollContentToEndLeftRunnable = it
                post(it)
            }
            return false
        }
        val content = this.contentView
        //没有 content, 不允许拖拽
        if (content == null) {
            resetDragMenuIfNeed()
            return false
        }
        //通过拖拽方向来确定要操作的 menu
        val menu = ensureDragMenuByDragEdge(dragEdge)
        //没有找到要拖拽的 menu 或者 menu 是禁用状态
        if (menu == null || !menu.isMenuEnabled) {
            resetDragMenuIfNeed()
            return false
        }
        val endLeft = getContentEndLeft(menu, open)
        val startLeft = content.left
        val dx = endLeft - startLeft
        if (dx == 0) {
            scroller.abortAnimation()
            resetDragMenuIfNeed()
            return true
        }
        if (smooth) {
            lastScrollX = startLeft
            val scrollRange = getContentScrollRange(menu)
            val duration = computeSettleDuration(dx, xVelocity, scrollRange)
            scroller.startScroll(startLeft, 0, dx, 0, duration)
            ViewCompat.postInvalidateOnAnimation(this)
        } else {
            scroller.abortAnimation()
            scrollContentBy(dx)
            resetDragMenuIfNeed()
        }
        return true
    }

    /**
     * 移除正在等待被执行的滑动操作
     */
    private fun removeScrollContentToEndLeftRunnableIfNeed() {
        scrollContentToEndLeftRunnable?.let {
            removeCallbacks(it)
            scrollContentToEndLeftRunnable = null
        }
    }

    private class ScrollContentToEndLeftRunnable(
        private val swipeMenuLayout: SwipeMenuLayout,
        private val dragEdge: DragEdge,
        private val open: Boolean,
        private val smooth: Boolean,
        private val xVelocity: Int
    ) : Runnable {
        override fun run() {
            swipeMenuLayout.scrollContentToEndLeft(dragEdge, open, smooth, xVelocity)
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val x = scroller.currX
            val deltaX = x - lastScrollX
            lastScrollX = x
            var shouldContinue = true
            if (deltaX != 0) {
                val unconsumed = scrollContentBy(deltaX)
                shouldContinue = unconsumed == 0
            }
            if (shouldContinue) {
                ViewCompat.postInvalidateOnAnimation(this)
            } else {
                scroller.abortAnimation()
                resetDragMenuIfNeed()
            }
        } else {
            resetDragMenuIfNeed()
        }
    }

    private fun computeSettleDuration(dx: Int, xVelocity: Int, motionRange: Int): Int {
        val xVel = clampMag(xVelocity, minimumVelocity, maximumVelocity)
        return computeAxisDuration(dx, xVel, motionRange)
    }

    private fun computeAxisDuration(delta: Int, velocity: Int, motionRange: Int): Int {
        if (delta == 0) {
            return 0
        }

        val halfWidth = width * 0.5f
        val distanceRatio = min(1f, abs(delta * 1f) / width)
        val distance = halfWidth + halfWidth * distanceInfluenceForSnapDuration(distanceRatio)

        val absVelocity = abs(velocity)
        val duration: Int = if (absVelocity > 0) {
            (2 * round(1000 * abs(distance / absVelocity))).toInt()
        } else {
            val range = abs(delta * 1f) / motionRange
            ((range + 1) * baseSettleDuration).toInt()
        }
        return min(duration, maxSettleDuration)
    }

    private fun distanceInfluenceForSnapDuration(value: Float): Float {
        var result = value
        result -= 0.5f // center the values about 0.
        result *= 0.3f * Math.PI.toFloat() / 2.0f
        return sin(result)
    }

    private fun clampMag(value: Int, absMin: Int, absMax: Int): Int {
        val absValue = abs(value)
        if (absValue < absMin) return 0
        if (absValue > absMax) {
            return if (value > 0) absMax else -absMax
        }
        return value
    }

    private fun getContentScrollRange(menu: View?): Int {
        if (contentView == null || menu == null || !menu.isMenuEnabled) {
            return 0
        }
        return menu.measuredWidth - menu.dragPadding
    }

    private fun ensureChild() {
        contentView = null
        leftMenuView = null
        rightMenuView = null
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val params = child.layoutParams as SwipeLayoutParams
            when (params.childType) {
                ChildType.Content -> {
                    if (contentView == null) {
                        contentView = child
                    } else {
                        throw IllegalStateException("SwipeMenuLayout can only have one Content")
                    }
                }
                ChildType.Menu -> {
                    when (params.dragEdge) {
                        DragEdge.Left -> {
                            if (leftMenuView == null) {
                                leftMenuView = child
                            } else {
                                throw IllegalStateException("SwipeMenuLayout can only have one LeftMenu")
                            }
                        }
                        DragEdge.Right -> {
                            if (rightMenuView == null) {
                                rightMenuView = child
                            } else {
                                throw IllegalStateException("SwipeMenuLayout can only have one RightMenu")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        ensureChild()
        super.onLayout(changed, left, top, right, bottom)
        layoutMenu()
    }

    private fun getContentScrollFraction(): Float {
        val content = this.contentView
        val menu = this.dragMenu
        if (content == null || menu == null) {
            return 0f
        }
        val contentScrollRange = getContentScrollRange(menu)
        return ((abs(content.left - paddingLeft)) * 1f) / contentScrollRange
    }

    private fun layoutMenu() {
        leftMenuView?.let { leftMenu ->
            when (leftMenu.showMode) {
                ShowMode.LayDown -> layoutMenuWhenLayDown(leftMenu)
                ShowMode.PullOut -> layoutMenuWhenPullOut(leftMenu)
            }
        }
        rightMenuView?.let { rightMenu ->
            when (rightMenu.showMode) {
                ShowMode.LayDown -> layoutMenuWhenLayDown(rightMenu)
                ShowMode.PullOut -> layoutMenuWhenPullOut(rightMenu)
            }
        }
    }

    private fun layoutMenuWhenLayDown(menu: View) {
        val params = menu.layoutParams as MarginLayoutParams
        val top = paddingTop + params.topMargin
        val bottom = height - paddingBottom - params.bottomMargin
        val left: Int
        val right: Int
        when (menu.dragEdge) {
            DragEdge.Left -> {
                left = paddingLeft + params.leftMargin
                right = left + menu.measuredWidth
            }
            DragEdge.Right -> {
                right = width - paddingRight - params.rightMargin
                left = right - menu.measuredWidth
            }
        }
        menu.layout(left, top, right, bottom)
    }

    private fun layoutMenuWhenPullOut(menu: View) {
        val params = menu.layoutParams as SwipeLayoutParams
        val contentScrollRange = getContentScrollRange(menu)
        val menuScrollRange = (contentScrollRange * params.dragParallax).toInt()
        val fraction = getContentScrollFraction()
        val top = paddingTop + params.topMargin
        val bottom = top + menu.measuredHeight
        val left: Int = when (menu.dragEdge) {
            DragEdge.Left -> {
                val startLeft = -menuScrollRange + params.leftMargin
                (startLeft + menuScrollRange * fraction).toInt()
            }
            DragEdge.Right -> {
                val offsetX = contentScrollRange - menuScrollRange
                val startLeft = width - params.rightMargin - offsetX
                (startLeft - menuScrollRange * fraction).toInt()
            }
        }
        val right = left + menu.measuredWidth
        menu.layout(left, top, right, bottom)
    }

    fun addOnSwipeChangedListener(listener: OnSwipeChangedListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeOnSwipeChangedListener(listener: OnSwipeChangedListener) {
        listeners.remove(listener)
    }

    fun removeAllOnSwipeChangedListeners() {
        listeners.clear()
    }

    private fun notifyStateChangeStartIfNeed(menu: View, dx: Int, consumed: Int) {
        if (dx != 0 && consumed != 0 && !isDragStart && menu.isMenuEnabled) {
            isDragStart = true
            val dragEdge = menu.dragEdge
            val isOpen = when (dragEdge) {
                DragEdge.Left -> dx > 0
                DragEdge.Right -> dx < 0
            }
            listeners.forEach {
                it.onDragStart(this, dragEdge, isOpen)
            }
        }
    }

    private fun isOpenPosition(content: View, menu: View): Boolean {
        return content.left == getContentEndLeft(menu, true)
    }

    private fun isClosePosition(content: View, menu: View): Boolean {
        return content.left == getContentEndLeft(menu, false)
    }

    private fun notifyStateChangedIfNeed(content: View, menu: View, consumed: Int) {
        val isOpenPosition = isOpenPosition(content, menu)
        val isClosePosition = isClosePosition(content, menu)
        when {
            //menu open
            isOpenPosition && !isMenuOpen && menu.isMenuEnabled -> {
                isMenuOpen = true
                listeners.forEach {
                    it.onOpen(this, menu.dragEdge)
                }
            }
            //menu close
            isClosePosition && isMenuOpen && menu.isMenuEnabled -> {
                isMenuOpen = false
                listeners.forEach {
                    it.onClose(this, menu.dragEdge)
                }
            }
        }
        //drag end
        if (consumed != 0 && (isOpenPosition || isClosePosition) && menu.isMenuEnabled) {
            isDragStart = false
            listeners.forEach {
                it.onDragEnd(this, menu.dragEdge)
            }
        }
    }

    private fun isTouchInContent(event: MotionEvent): Boolean {
        val content = this.contentView ?: return false
        content.getGlobalVisibleRect(rect)
        return rect.contains(event.rawX.toInt(), event.rawY.toInt()).apply {
            rect.setEmpty()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        //移除正在等待被执行的滑动操作
        removeScrollContentToEndLeftRunnableIfNeed()
    }

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return SwipeLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(lp: ViewGroup.LayoutParams): ViewGroup.LayoutParams {
        return when (lp) {
            is SwipeLayoutParams -> SwipeLayoutParams(lp)
            is MarginLayoutParams -> SwipeLayoutParams(lp)
            else -> SwipeLayoutParams(lp)
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return SwipeLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is SwipeLayoutParams
    }

    class SwipeLayoutParams : LayoutParams {

        var childType: ChildType = ChildType.Content
        var dragEdge: DragEdge = DragEdge.Left
        var showMode: ShowMode = ShowMode.LayDown
        var dragParallax: Float = 1f
        var dragPadding: Int = 0

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.SwipeMenuLayout_Layout)
            val childTypeIndex = ta.getInt(R.styleable.SwipeMenuLayout_Layout_sml_childType, 0)
            childType = ChildType.values()[childTypeIndex]
            val dragEdgeIndex = ta.getInt(R.styleable.SwipeMenuLayout_Layout_sml_dragEdge, 0)
            dragEdge = DragEdge.values()[dragEdgeIndex]
            val showModeIndex = ta.getInt(R.styleable.SwipeMenuLayout_Layout_sml_showMode, 0)
            showMode = ShowMode.values()[showModeIndex]
            dragParallax = ta.getFloat(R.styleable.SwipeMenuLayout_Layout_sml_dragParallax, 1f)
            dragParallax = MathUtils.clamp(dragParallax, 0f, 1f)
            if (dragParallax == 0f && showMode != ShowMode.LayDown) {
                showMode = ShowMode.LayDown
            }
            dragPadding = max(ta.getDimensionPixelOffset(R.styleable.SwipeMenuLayout_Layout_sml_dragPadding, 0), 0)
            ta.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height)

        constructor(
            width: Int,
            height: Int,
            childType: ChildType,
            dragEdge: DragEdge,
            showMode: ShowMode,
            dragParallax: Float,
            dragPadding: Int,
        ) : super(width, height) {
            this.childType = childType
            this.dragEdge = dragEdge
            this.showMode = showMode
            this.dragParallax = dragParallax
            this.dragPadding = dragPadding
        }

        constructor(source: ViewGroup.LayoutParams) : super(source)

        constructor(source: MarginLayoutParams) : super(source)

        constructor(source: SwipeLayoutParams) : super(source) {
            this.childType = source.childType
            this.dragEdge = source.dragEdge
            this.showMode = source.showMode
            this.dragParallax = source.dragParallax
            this.dragPadding = source.dragPadding
        }
    }

    enum class DragEdge {
        Left, Right
    }

    enum class ChildType {
        Content, Menu
    }

    enum class ShowMode {
        LayDown, PullOut
    }

    interface OnSwipeChangedListener {

        /**
         * 已经 open
         */
        fun onOpen(layout: SwipeMenuLayout, openEdge: DragEdge)

        /**
         * 已经 close
         */
        fun onClose(layout: SwipeMenuLayout, closeEdge: DragEdge)

        /**
         * 开始拖拽
         */
        fun onDragStart(layout: SwipeMenuLayout, dragEdge: DragEdge, isOpen: Boolean)

        /**
         * 已经拖拽结束
         */
        fun onDragEnd(layout: SwipeMenuLayout, dragEdge: DragEdge)
    }

}