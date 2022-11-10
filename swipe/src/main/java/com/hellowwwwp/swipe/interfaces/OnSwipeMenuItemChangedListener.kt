package com.hellowwwwp.swipe.interfaces

import androidx.recyclerview.widget.RecyclerView
import com.hellowwwwp.swipe.SwipeMenuLayout

/**
 * @author: wangpan
 * @email: p.wang0813@gmail.com
 * @date: 2022/11/10
 */
interface OnSwipeMenuItemChangedListener<VH : RecyclerView.ViewHolder> {

    fun onSwipeMenuLayoutDragStart(
        holder: VH,
        layout: SwipeMenuLayout,
        dragEdge: SwipeMenuLayout.DragEdge,
        isOpen: Boolean
    )

    fun onSwipeMenuLayoutDragEnd(
        holder: VH,
        layout: SwipeMenuLayout,
        dragEdge: SwipeMenuLayout.DragEdge
    )

    fun onSwipeMenuLayoutOpen(
        holder: VH,
        layout: SwipeMenuLayout,
        openEdge: SwipeMenuLayout.DragEdge
    )

    fun onSwipeMenuLayoutClose(
        holder: VH,
        layout: SwipeMenuLayout,
        closeEdge: SwipeMenuLayout.DragEdge
    )

}