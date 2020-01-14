package com.crimson.mvvm.base

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.crimson.mvvm.R
import com.crimson.mvvm.config.ViewLifeCycleExt
import com.crimson.mvvm.ext.runOnIO
import com.crimson.mvvm.utils.SDKVersionUtils
import com.crimson.mvvm.utils.StatusBarUtils

/**
 * activity lifecycle
 * 全局的activity生命周期回调，可以在这里做很多事情
 *
 */
open class BaseActivityLifecycle : ActivityLifecycleCallbacks {

    private val fragmentLifeCycle = BaseFragmentLifeCycle()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

        //初始全局默认化状态栏
        initDefaultStatusBar(activity)
        //初始化全局默认标题栏
        initDefaultTitleBar(activity)
        //初始化全局contentView函数调用
        initDefaultView(activity)

        runOnIO {
            //添加activity入站
            ViewLifeCycleExt.addActivityToStack(activity)
            if (activity is FragmentActivity) {
                //注册fragment生命周期
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                    fragmentLifeCycle,
                    true
                )
            }
        }


    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {

        runOnIO {
            ViewLifeCycleExt.removeActivityFromStack(activity)
            if (activity is FragmentActivity) {
                activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(
                    fragmentLifeCycle
                )
            }
        }

    }

    /**
     * 初始化默认状态栏
     * 6.0以上为默认模式默认为底色为白色的亮色模式
     * 这种情况下是状态栏可见且会导致布局上移，顶层view还需设置 android:fitsSystemWindows="true"
     */
    private fun initDefaultStatusBar(activity: Activity) {

        if (activity is IStatusBar) {

            if (SDKVersionUtils.isAboveAndroid6()) {
                StatusBarUtils.setColor(
                    activity,
                    ContextCompat.getColor(activity, android.R.color.white),
                    0
                )
                StatusBarUtils.setLightMode(activity)
                //获取contentView设置fitsSystemWindows
                activity.findViewById<FrameLayout>(android.R.id.content).getChildAt(0)
                    .fitsSystemWindows = true
            } else {
                StatusBarUtils.setColor(
                    activity,
                    ContextCompat.getColor(activity, android.R.color.white),
                    68
                )
            }

            activity.initStatusBar()
        }


    }

    /**
     * 初始化默认标题栏
     *
     */
    private fun initDefaultTitleBar(activity: Activity) {

        if (activity is ITitleBar) {

            activity.findViewById<Toolbar>(R.id.title_bar)?.run {
                //设置toolBar为ActionBar
                if (activity is AppCompatActivity) {
                    activity.setSupportActionBar(this)
                    activity.supportActionBar?.setDisplayShowTitleEnabled(false)
                    val backIconRes = activity.initBackIconRes()
                    if (backIconRes != null && backIconRes != 0) {
                        setNavigationIcon(backIconRes)

                    } else {
                        //左侧添加一个默认的返回图标
                        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
                        //设置返回键可用
                        activity.supportActionBar?.setHomeButtonEnabled(true)
                    }

                    setNavigationOnClickListener {
                        activity.finish()
                    }

                }
            }

            //设置title
            activity.findViewById<AppCompatTextView>(R.id.title_bar_text)?.run {
                val titleText = activity.initTitleText()
                text = titleText
            }

            activity.initTitleBar()

            /**
             * 如果想添加menu，请在Activity中重写initMenuRes 和 onMenuItemSelected
             */

        }
    }

    /**
     * 初始化全局contentView函数调用
     */
    private fun initDefaultView(activity: Activity) {
        if (activity is IView) {
            activity.initView()
            activity.initData()
            activity.initViewObservable()

        }
    }
}


