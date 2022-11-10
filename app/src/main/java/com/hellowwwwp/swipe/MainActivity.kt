package com.hellowwwwp.swipe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.hellowwwwp.swipe.databinding.ActivityMainBinding
import com.hellowwwwp.swipe.demo.ListDoubleRightMenuActivity
import com.hellowwwwp.swipe.demo.ListSingleLeftRightMenuActivity
import com.hellowwwwp.swipe.demo.ListSingleRightMenuActivity
import com.hellowwwwp.swipe.demo.SimpleDemoActivity

class MainActivity : AppCompatActivity() {

    private val viewBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        with(viewBinding) {
            //简单示例
            btnDemo1.setOnClickListener {
                startActivity(Intent(this@MainActivity, SimpleDemoActivity::class.java))
            }
            //列表-右边单个菜单
            btnDemo2.setOnClickListener {
                startActivity(Intent(this@MainActivity, ListSingleRightMenuActivity::class.java))
            }
            //列表-左右单个菜单
            btnDemo3.setOnClickListener {
                startActivity(Intent(this@MainActivity, ListSingleLeftRightMenuActivity::class.java))
            }
            //列表-右边两个菜单
            btnDemo4.setOnClickListener {
                startActivity(Intent(this@MainActivity, ListDoubleRightMenuActivity::class.java))
            }
        }
    }

}