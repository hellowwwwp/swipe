package com.hellowwwwp.swipe.demo

import androidx.recyclerview.widget.DiffUtil

/**
 * @author: wangpan
 * @email: p.wang0813@gmail.com
 * @date: 2022/11/10
 */
data class MyItem(
    val content: String,
    val leftMenuStatus: Boolean = false,
    val rightMenuStatus: Boolean = false,
    val rightMenuText: String = "right menu",
    val rightMenu2Text: String = "right menu2",
    val leftMenuText: String = "left menu",
) {
    companion object {
        val comparator = object : DiffUtil.ItemCallback<MyItem>() {
            override fun areItemsTheSame(oldItem: MyItem, newItem: MyItem): Boolean {
                return oldItem.content == newItem.content
            }

            override fun areContentsTheSame(oldItem: MyItem, newItem: MyItem): Boolean {
                return oldItem == newItem
            }
        }
    }

}