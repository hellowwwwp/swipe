package com.hellowwwwp.swipe

import android.view.View
import androidx.collection.ArrayMap
import androidx.recyclerview.widget.RecyclerView
import com.hellowwwwp.swipe.interfaces.OnSwipeMenuItemChangedListener
import com.hellowwwwp.swipe.interfaces.SimpleOnAttachStateChangeListener

/**
 * @author: wangpan
 * @email: p.wang0813@gmail.com
 * @date: 2022/11/10
 *
 * 配合 RecyclerView 使用, 保证当前只能打开一个 SwipeMenuLayout
 */
@Suppress("unused")
class SwipeMenuItemRecycleManager<VH : RecyclerView.ViewHolder> : SwipeMenuLayout.OnSwipeChangedListener {

    private var recyclerView: RecyclerView? = null

    var swipeMenuLayoutFinder: ((VH) -> SwipeMenuLayout?)? = null

    private var currentSwipeMenuItem: SwipeMenuItem<VH>? = null

    private val bindSwipeMenuItemMap: ArrayMap<SwipeMenuLayout, VH> = ArrayMap()

    private val listeners: MutableList<OnSwipeMenuItemChangedListener<VH>> = mutableListOf()

    val currentSwipeMenuLayout: SwipeMenuLayout?
        get() = currentSwipeMenuItem?.layout

    val currentViewHolder: VH?
        get() = currentSwipeMenuItem?.holder

    val currentPosition: Int
        get() = currentSwipeMenuItem?.position ?: RecyclerView.NO_POSITION

    private val onAttachStateChangeListener = object : SimpleOnAttachStateChangeListener() {
        override fun onViewDetachedFromWindow(view: View) {
            unbindRecyclerView()
        }
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                closeCurrentSwipeMenuItem()
            }
        }
    }

    fun addOnSwipeMenuItemChangedListener(listener: OnSwipeMenuItemChangedListener<VH>) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeOnSwipeMenuItemChangedListener(listener: OnSwipeMenuItemChangedListener<VH>) {
        listeners.remove(listener)
    }

    fun bindRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        //禁止在列表中同时操作多个 item
        recyclerView.isMotionEventSplittingEnabled = false
        recyclerView.addOnAttachStateChangeListener(onAttachStateChangeListener)
        recyclerView.addOnScrollListener(onScrollListener)
    }

    fun unbindRecyclerView() {
        recyclerView?.let {
            it.removeOnAttachStateChangeListener(onAttachStateChangeListener)
            it.removeOnScrollListener(onScrollListener)
            recyclerView = null
        }
    }

    fun clear() {
        currentSwipeMenuItem = null
        bindSwipeMenuItemMap.clear()
    }

    fun bindSwipeMenuItem(holder: VH) {
        swipeMenuLayoutFinder?.invoke(holder)?.let { layout ->
            //开启严格拦截事件模式(避免和 RecyclerView 抢垂直方向的滑动事件)
            layout.isStrictInterceptEnabled = true
            layout.addOnSwipeChangedListener(this)
            bindSwipeMenuItemMap.put(layout, holder)
        }
    }

    fun unbindSwipeMenuItem(holder: VH) {
        swipeMenuLayoutFinder?.invoke(holder)?.let { layout ->
            layout.removeOnSwipeChangedListener(this)
            bindSwipeMenuItemMap.remove(layout, holder)
        }
    }

    fun tryCloseSwipeMenuItem(holder: VH, dragEdge: SwipeMenuLayout.DragEdge? = null, smooth: Boolean = true) {
        swipeMenuLayoutFinder?.invoke(holder)?.let { layout ->
            if (layout == currentSwipeMenuItem?.layout) {
                closeCurrentSwipeMenuItem(dragEdge, smooth)
            } else {
                layout.close(dragEdge, smooth)
            }
        }
    }

    fun closeCurrentSwipeMenuItem(closeEdge: SwipeMenuLayout.DragEdge? = null, smooth: Boolean = true) {
        currentSwipeMenuItem?.apply {
            close(closeEdge, smooth)
            currentSwipeMenuItem = null
        }
    }

    override fun onDragStart(layout: SwipeMenuLayout, dragEdge: SwipeMenuLayout.DragEdge, isOpen: Boolean) {
        bindSwipeMenuItemMap[layout]?.let { holder ->
            if (isOpen) {
                closeCurrentSwipeMenuItem()
                currentSwipeMenuItem = SwipeMenuItem(holder, layout)
            }
            listeners.forEach {
                it.onSwipeMenuLayoutDragStart(holder, layout, dragEdge, isOpen)
            }
        }
    }

    override fun onDragEnd(layout: SwipeMenuLayout, dragEdge: SwipeMenuLayout.DragEdge) {
        bindSwipeMenuItemMap[layout]?.let { holder ->
            if (layout.isOpen) {
                if (layout != currentSwipeMenuItem?.layout) {
                    closeCurrentSwipeMenuItem()
                    currentSwipeMenuItem = SwipeMenuItem(holder, layout)
                }
            } else {
                if (layout == currentSwipeMenuItem?.layout) {
                    closeCurrentSwipeMenuItem()
                }
            }
            listeners.forEach {
                it.onSwipeMenuLayoutDragEnd(holder, layout, dragEdge)
            }
        }
    }

    override fun onOpen(layout: SwipeMenuLayout, openEdge: SwipeMenuLayout.DragEdge) {
        bindSwipeMenuItemMap[layout]?.let { holder ->
            listeners.forEach {
                it.onSwipeMenuLayoutOpen(holder, layout, openEdge)
            }
        }
    }

    override fun onClose(layout: SwipeMenuLayout, closeEdge: SwipeMenuLayout.DragEdge) {
        bindSwipeMenuItemMap[layout]?.let { holder ->
            listeners.forEach {
                it.onSwipeMenuLayoutClose(holder, layout, closeEdge)
            }
        }
    }

    private class SwipeMenuItem<VH : RecyclerView.ViewHolder>(
        val holder: VH,
        val layout: SwipeMenuLayout
    ) {
        val position: Int
            get() = holder.bindingAdapterPosition

        fun close(closeEdge: SwipeMenuLayout.DragEdge? = null, smooth: Boolean = true): Boolean {
            return layout.close(closeEdge, smooth)
        }
    }

}