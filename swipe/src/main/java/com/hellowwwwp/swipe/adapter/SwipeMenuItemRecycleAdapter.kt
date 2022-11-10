package com.hellowwwwp.swipe.adapter

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.hellowwwwp.swipe.SwipeMenuItemRecycleManager
import com.hellowwwwp.swipe.SwipeMenuLayout
import com.hellowwwwp.swipe.interfaces.OnSwipeMenuItemChangedListener

/**
 * @author: wangpan
 * @email: p.wang0813@gmail.com
 * @date: 2022/11/10
 */
@Suppress("unused")
abstract class SwipeMenuItemRecycleAdapter<VH : ViewHolder> : RecyclerView.Adapter<VH>(),
    OnSwipeMenuItemChangedListener<VH> {

    protected val manager = SwipeMenuItemRecycleManager<VH>().also {
        it.swipeMenuLayoutFinder = ::findSwipeMenuLayout
        it.addOnSwipeMenuItemChangedListener(this)
    }

    fun closeCurrentSwipeMenuItem(closeEdge: SwipeMenuLayout.DragEdge? = null, smooth: Boolean = true) {
        manager.closeCurrentSwipeMenuItem(closeEdge, smooth)
    }

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        manager.bindRecyclerView(recyclerView)
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        manager.unbindRecyclerView()
        manager.clear()
    }

    abstract fun findSwipeMenuLayout(holder: VH): SwipeMenuLayout?

    @CallSuper
    override fun onBindViewHolder(holder: VH, position: Int) {
        manager.bindSwipeMenuItem(holder)
    }

    @CallSuper
    override fun onViewDetachedFromWindow(holder: VH) {
        manager.tryCloseSwipeMenuItem(holder, smooth = false)
    }

    @CallSuper
    override fun onViewRecycled(holder: VH) {
        manager.unbindSwipeMenuItem(holder)
    }

    override fun onSwipeMenuLayoutDragStart(
        holder: VH,
        layout: SwipeMenuLayout,
        dragEdge: SwipeMenuLayout.DragEdge,
        isOpen: Boolean
    ) {
        //no op
    }

    override fun onSwipeMenuLayoutDragEnd(
        holder: VH,
        layout: SwipeMenuLayout,
        dragEdge: SwipeMenuLayout.DragEdge
    ) {
        //no op
    }

    override fun onSwipeMenuLayoutOpen(
        holder: VH,
        layout: SwipeMenuLayout,
        openEdge: SwipeMenuLayout.DragEdge
    ) {
        //no op
    }

    override fun onSwipeMenuLayoutClose(
        holder: VH,
        layout: SwipeMenuLayout,
        closeEdge: SwipeMenuLayout.DragEdge
    ) {
        //no op
    }

}