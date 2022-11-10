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
import com.hellowwwwp.swipe.databinding.ActivityListDoubleRightMenuBinding
import com.hellowwwwp.swipe.databinding.ActivityListSingleRightMenuBinding
import com.hellowwwwp.swipe.databinding.LayoutDoubleRightMenuItemViewBinding
import com.hellowwwwp.swipe.databinding.LayoutSingleRightMenuItemViewBinding

class ListDoubleRightMenuActivity : AppCompatActivity() {

    private val viewBinding: ActivityListDoubleRightMenuBinding by lazy {
        ActivityListDoubleRightMenuBinding.inflate(layoutInflater)
    }

    private val myAdapter: MyAdapter = MyAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initView()
    }

    private fun initView() {
        with(viewBinding) {
            rcvList.layoutManager = LinearLayoutManager(this@ListDoubleRightMenuActivity)
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
            val binding = LayoutDoubleRightMenuItemViewBinding.inflate(inflater, parent, false)
            return MyViewHolder(binding).apply {
                viewBinding.tvContent.setOnClickListener {
                    val position = layoutPosition
                    if (position != RecyclerView.NO_POSITION) {
                        ToastUtils.showShort("content: $position")
                    }
                }
                viewBinding.tvRightMenu1.setOnClickListener {
                    val position = layoutPosition
                    if (position != RecyclerView.NO_POSITION) {
                        ToastUtils.showShort("right menu1: $position")
                    }
                }
                viewBinding.tvRightMenu2.setOnClickListener {
                    val position = layoutPosition
                    if (position != RecyclerView.NO_POSITION) {
                        ToastUtils.showShort("right menu2: $position")
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
                    tvRightMenu1.text = item.rightMenuText
                    tvRightMenu2.text = item.rightMenu2Text
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
        val viewBinding: LayoutDoubleRightMenuItemViewBinding
    ) : RecyclerView.ViewHolder(viewBinding.root)

}