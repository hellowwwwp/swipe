package com.hellowwwwp.swipe.demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.blankj.utilcode.util.ToastUtils
import com.hellowwwwp.swipe.R
import com.hellowwwwp.swipe.databinding.ActivitySimpleDemoBinding

class SimpleDemoActivity : AppCompatActivity() {

    private val viewBinding: ActivitySimpleDemoBinding by lazy {
        ActivitySimpleDemoBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        with(viewBinding) {
            tvContent1.setOnClickListener {
                ToastUtils.showShort("${tvContent1.text}")
            }
            tvRightMenu1.setOnClickListener {
                sml1.close()
                ToastUtils.showShort("${tvRightMenu1.text}")
            }
            tvContent2.setOnClickListener {
                ToastUtils.showShort("${tvContent2.text}")
            }
            tvRightMenu2.setOnClickListener {
                sml2.close()
                ToastUtils.showShort("${tvRightMenu2.text}")
            }
        }
    }

}