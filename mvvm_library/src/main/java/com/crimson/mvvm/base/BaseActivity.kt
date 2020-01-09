@file:Suppress("UNCHECKED_CAST")

package com.crimson.mvvm.base

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import com.crimson.mvvm.config.AppConfigOptions
import com.crimson.mvvm.ext.tryCatch
import com.trello.rxlifecycle3.components.support.RxAppCompatActivity
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * @author crimson
 * @date 19/12/15
 * base Activity
 * 默认单个ViewModel,不需要koin注入，如果想一个activity有多个ViewModel,可以用viewModelModule创建viewModel并注入
 */
abstract class BaseActivity<VB : ViewDataBinding, VM : BaseViewModel> : RxAppCompatActivity(),
    IView {

    var vb: VB? = null

    var vm: VM? = null

    var loadingView: IViewDataLoading? = null

    protected val context: Context? by lazy { this }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initViewBinding(savedInstanceState)
        initViewModelLiveDataObserver()
        initView()
        initData()
        initViewObservable()

    }

    /**
     * 初始化ViewBinding 和 ViewModel
     */
    private fun initViewBinding(savedInstanceState: Bundle?) {

        vb = DataBindingUtil.setContentView(this, initContentView(savedInstanceState))
        vm = initViewModel()
        if (vm == null) {
            val type: Type? = javaClass.genericSuperclass
            if (type is ParameterizedType && type.actualTypeArguments.size == 2) {
                val viewModel = type.actualTypeArguments[1] as Class<VM>
                vm = viewModel.newInstance()
            }
        }

        vb?.run {
            lifecycleOwner = this@BaseActivity
            val vmId = initViewModelId()
            setVariable(vmId, vm)
        }

        //关联ViewModel
        vm?.run {
            //让ViewModel拥有View的生命周期
            lifecycle.addObserver(this)
            //注入RxLifecycle生命周期
            rxlifecycle = this@BaseActivity
            //注册RxBus
            registerRxBus()
        }
    }

    /**
     * 初始化view model中的LiveData call
     */
    private fun initViewModelLiveDataObserver() {

        vm?.onLoadingViewInjectToRootLD?.observe(this, Observer {
            onLoadingViewInjectToRoot()
        })

        vm?.onLoadingViewResultLD?.observe(this, Observer {
            onLoadingViewResult()
        })

        vm?.dataLoadingLD?.observe(this, Observer {
            onDataLoading(it)
        })

        vm?.dataResultLD?.observe(this, Observer {
            onDataResult()
        })

        vm?.dataLoadingErrorLD?.observe(this, Observer {
            onLoadingError()
        })

        vm?.viewFinishedLD?.observe(this, Observer {
            finish()
        })

    }

    /**
     * run on view create with get data
     */
    open fun onLoadingViewInjectToRoot() {
        checkLoadingViewImpl()
        loadingView?.onLoadingViewInjectToRoot(vb?.root?.parent as? ViewGroup)
    }

    /**
     * run on view get data finish
     */
    open fun onLoadingViewResult() {
        loadingView?.onLoadingViewResult(vb?.root?.parent as? ViewGroup)
    }

    /**
     * run on data loading
     */
    open fun onDataLoading(it: String?) {
        checkLoadingViewImpl()
        loadingView?.onDataLoading(it)

    }

    /**
     * run on data loading finish
     */
    open fun onDataResult() {
        loadingView?.onDataLoadingResult()
    }


    /**
     * data loading error
     */
    open fun onLoadingError() {
        checkLoadingViewImpl()
        loadingView?.onLoadingError(vb?.root?.parent as? ViewGroup)
    }

    /**
     * 检查loadingView的实现
     */
    private fun checkLoadingViewImpl() {
        if (loadingView == null) {
            val clazz = AppConfigOptions.LOADING_VIEW_CLAZZ
            if (clazz != null) {
                tryCatch {
                    val constructor = clazz.getConstructor(Context::class.java)
                    loadingView = constructor.newInstance(this)
                }
            }
            if (loadingView == null) {
                loadingView = CommonViewLoading(this)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        //移除ViewModel生命周期
        vm?.let {
            lifecycle.removeObserver(it)
            it.removeRxBus()
            null
        }
        vb?.apply {
            unbind()
        }
        loadingView?.let {
            null
        }

    }

    /**
     * 初始化根布局
     *
     * @return 布局layout的id
     */
    abstract fun initContentView(savedInstanceState: Bundle?): Int

    /**
     * 初始化ViewModel的id
     *
     * @return BR的id
     */
    abstract fun initViewModelId(): Int

    /**
     * 初始化viewModel
     */
    open fun initViewModel(): VM? = null

    override fun initView() {}
    override fun initData() {}
    override fun initViewObservable() {}


}




