package com.hellowwwwp.swipe.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ToastUtils
import com.hellowwwwp.swipe.R
import com.hellowwwwp.swipe.SwipeMenuLayout
import com.hellowwwwp.swipe.adapter.SwipeMenuItemRecycleListAdapter
import com.hellowwwwp.swipe.databinding.ActivityListSingleLeftRightMenuBinding
import com.hellowwwwp.swipe.databinding.ActivityListSingleRightMenuBinding
import com.hellowwwwp.swipe.databinding.LayoutSingleLeftRightMenuItemViewBinding
import com.hellowwwwp.swipe.databinding.LayoutSingleRightMenuItemViewBinding

class ListSingleLeftRightMenuActivity : AppCompatActivity() {

    private val viewBinding: ActivityListSingleLeftRightMenuBinding by lazy {
        ActivityListSingleLeftRightMenuBinding.inflate(layoutInflater)
    }

    private val myAdapter: MyAdapter = MyAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initView()
    }

    private fun initView() {
        with(viewBinding) {
            rcvList.layoutManager = LinearLayoutManager(this@ListSingleLeftRightMenuActivity)
            rcvList.adapter = myAdapter
            myAdapter.submitList(getTestData())
        }
    }

    private fun getTestData(): List<MyItem> {
        val items = mutableListOf<MyItem>()
        repeat(100) {
            items.add(MyItem(content = "item index: $it"))
        }
        return items
    }

    class MyAdapter : SwipeMenuItemRecycleListAdapter<MyItem, RecyclerView.ViewHolder>(MyItem.comparator) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = LayoutSingleLeftRightMenuItemViewBinding.inflate(inflater, parent, false)
            return MyViewHolder(binding).apply {
                viewBinding.tvContent.setOnClickListener {
                    val position = layoutPosition
                    if (position != RecyclerView.NO_POSITION) {
                        ToastUtils.showShort("content: $position")
                    }
                }
                viewBinding.tvLeftMenu.setOnClickListener {
                    val position = layoutPosition
                    if (position != RecyclerView.NO_POSITION) {
                        ToastUtils.showShort("left menu: $position")
                    }
                }
                viewBinding.tvRightMenu.setOnClickListener {
                    val position = layoutPosition
                    if (position != RecyclerView.NO_POSITION) {
                        ToastUtils.showShort("right menu: $position")
                    }
                }
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            val item = getItem(position)
            if (holder is MyViewHolder) {
                with(holder.viewBinding) {
                    tvContent.text = item.content
                    tvLeftMenu.text = item.leftMenuText
                    if (item.leftMenuStatus) {
                        sml.open(SwipeMenuLayout.DragEdge.Left, false)
                    } else {
                        sml.close(SwipeMenuLayout.DragEdge.Left, false)
                    }
                    tvRightMenu.text = item.rightMenuText
                    if (item.rightMenuStatus) {
                        sml.open(SwipeMenuLayout.DragEdge.Right, false)
                    } else {
                        sml.close(SwipeMenuLayout.DragEdge.Right, false)
                    }
                }
            }
        }

        override fun findSwipeMenuLayout(holder: RecyclerView.ViewHolder): SwipeMenuLayout? {
            if (holder is MyViewHolder) {
                return holder.viewBinding.sml
            }
            return null
        }

        override fun onSwipeMenuLayoutOpen(
            holder: RecyclerView.ViewHolder,
            layout: SwipeMenuLayout,
            openEdge: SwipeMenuLayout.DragEdge
        ) {
            ToastUtils.showShort("open: position=${holder.layoutPosition}, openEdge=$openEdge")
        }

        override fun onSwipeMenuLayoutClose(
            holder: RecyclerView.ViewHolder,
            layout: SwipeMenuLayout,
            closeEdge: SwipeMenuLayout.DragEdge
        ) {
            ToastUtils.showShort("close: position=${holder.layoutPosition}, closeEdge=$closeEdge")
        }
    }

    class MyViewHolder(
        val viewBinding: LayoutSingleLeftRightMenuItemViewBinding
    ) : RecyclerView.ViewHolder(viewBinding.root)
}