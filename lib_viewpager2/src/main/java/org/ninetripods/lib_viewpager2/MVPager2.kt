package org.ninetripods.lib_viewpager2

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import org.ninetripods.lib_viewpager2.adapter.DELAY_INTERVAL_TIME
import org.ninetripods.lib_viewpager2.adapter.MVP2Adapter
import org.ninetripods.lib_viewpager2.adapter.SIDE_NUM
import org.ninetripods.lib_viewpager2.imageLoader.DefaultLoader
import org.ninetripods.lib_viewpager2.imageLoader.IClickListener

fun log(message: String) {
    Log.e("TTT", message)
}

class MVPager2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {


    private var mViewPager2: ViewPager2
    private var mOnPageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var mRealCount: Int = 0 //VP2真实数量
    private lateinit var mVP2Adapter: MVP2Adapter<String>
    private var mModels: ArrayList<String> = ArrayList()
    private var mExtendModels: ArrayList<String> = ArrayList()
    private var mIsAutoPlay = true //自动轮播
    private var mCurPos = SIDE_NUM //当前滑动到的位置
    private var mClickListener: IClickListener? = null

    private var mItemPaddingLeft: Int = 0 //Item之间的padding间隔
    private var mItemPaddingRight: Int = 0
    private var mItemPaddingTop: Int = 0
    private var mItemPaddingBottom: Int = 0

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_mvpager2, this)
        mViewPager2 = findViewById(R.id.vp_pager2)
    }

    fun setDatas(list: ArrayList<String>): MVPager2 {
        this.mModels = list
        mRealCount = mModels.size
        extendOriginModels()
        return this
    }

    fun setAutoPlay(isAutoPlay: Boolean): MVPager2 {
        this.mIsAutoPlay = isAutoPlay
        return this
    }

    fun start() {
        initMVPager2()
        mVP2Adapter = MVP2Adapter()
        mVP2Adapter.setData(mExtendModels)
        mVP2Adapter.setImageLoader(DefaultLoader())
        mVP2Adapter.setOnItemClickListener(mClickListener)
        mViewPager2.adapter = mVP2Adapter
        mViewPager2.setCurrentItem(SIDE_NUM, false)
        if (mIsAutoPlay) startAutoPlay()
    }

    fun setOnBannerClickListener(listener: IClickListener): MVPager2 {
        this.mClickListener = listener
        return this
    }

    fun setItemPadding(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): MVPager2 {
        this.mItemPaddingLeft = left
        this.mItemPaddingTop = top
        this.mItemPaddingRight = right
        this.mItemPaddingBottom = bottom
        return this
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) {
            if (mIsAutoPlay) startAutoPlay()
        } else {
            if (mIsAutoPlay) stopAutoPlay()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (mIsAutoPlay) {
            val action = ev.action
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
                startAutoPlay()
            } else if (action == MotionEvent.ACTION_DOWN) {
                stopAutoPlay()
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    fun registerOnPageChangeCallback(callback: ViewPager2.OnPageChangeCallback) {
        this.mOnPageChangeCallback = callback
    }

    private val autoRunnable: Runnable = object : Runnable {
        override fun run() {
            if (mRealCount > 1 && mIsAutoPlay) {
                mCurPos = mCurPos % mExtendModels.size + 1
                when (mCurPos) {
                    1 -> {
                        //TODO 待优化 自动轮播时会有一瞬间显得顿一下
                        //滑动到第2个时 自动滑动至倒数第3个
                        mViewPager2.setCurrentItem(exThreeLastPos(), false)
                    }
                    exSecondLastPos() -> {
                        mViewPager2.setCurrentItem(SIDE_NUM, false)
                    }
                    else -> {
                        mViewPager2.currentItem = mCurPos
                    }
                }
                postDelayed(this, DELAY_INTERVAL_TIME)
            }
        }

    }

    private fun startAutoPlay() {
        removeCallbacks(autoRunnable)
        postDelayed(autoRunnable, DELAY_INTERVAL_TIME)
    }

    private fun stopAutoPlay() {
        removeCallbacks(autoRunnable)
    }

    /**
     * 扩展原有数据
     */
    private fun extendOriginModels() {
        mExtendModels.clear()
        if (mRealCount > 1) {
            //真实数量必须大于1
            for (i in 0..exFirstLastPos()) {
                when (i) {
                    0 -> {
                        mExtendModels.add(mModels[mRealCount - 2])
                    }
                    1 -> {
                        mExtendModels.add(mModels[mRealCount - 1])
                    }
                    exSecondLastPos() -> {
                        mExtendModels.add(mModels[0])
                    }
                    exFirstLastPos() -> {
                        mExtendModels.add(mModels[1])
                    }
                    else -> {
                        mExtendModels.add(mModels[i - 2])
                    }
                }
            }
        } else {
            mExtendModels.add(mModels[0])
        }
    }

    private fun initMVPager2() {
        mViewPager2.offscreenPageLimit = 1
        mViewPager2.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        //CompositePageTransformer可以同时添加多个ViewPager2.PageTransformer
        val multiTransformer = CompositePageTransformer()
        //multiTransformer.addTransformer(ScaleInTransformer())
        //设置Margin
        multiTransformer.addTransformer(MarginPageTransformer(1))
        mViewPager2.setPageTransformer(multiTransformer)

        /**
         * ViewPager2源码第254行,RecyclerView固定索引为0：
         * attachViewToParent(mRecyclerView, 0, mRecyclerView.getLayoutParams());
         */
        val innerRecyclerView = mViewPager2.getChildAt(0) as RecyclerView
        innerRecyclerView.apply {
            //这里会导致离屏预加载数+1
            setPadding(mItemPaddingLeft, mItemPaddingTop, mItemPaddingRight, mItemPaddingBottom)
            clipToPadding = false
            clipChildren = false
            //设置mCachedViews缓存大小 默认是2
            //setItemViewCacheSize(2)
        }

        mViewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                mOnPageChangeCallback?.onPageScrolled(
                    position,
                    positionOffset,
                    positionOffsetPixels
                )
            }

            override fun onPageScrollStateChanged(state: Int) {
                mOnPageChangeCallback?.onPageScrollStateChanged(state)
                if (mRealCount > 1 && (state == ViewPager2.SCROLL_STATE_IDLE)) {
                    if (mViewPager2.currentItem == exSecondPositive()) {
                        //向左滑动，滑动到正数第2个时 自动将转换到倒数第3的位置(该位置为真实数量的最后一个)
                        mViewPager2.setCurrentItem(exThreeLastPos(), false)
                    } else if (mViewPager2.currentItem == exSecondLastPos()) {
                        //向右滑动，滑动到倒数第2个时 自动将转换到正数第3的位置(该位置为真实数量的第一个)
                        mViewPager2.setCurrentItem(SIDE_NUM, false)
                    }
                }
            }

            override fun onPageSelected(position: Int) {
                mCurPos = position
                mOnPageChangeCallback?.onPageSelected(position)
            }
        })
    }

    //扩展之后的倒数第1条数据
    private fun exFirstLastPos(): Int = mRealCount + 3

    //扩展之后的倒数第2条数据
    private fun exSecondLastPos(): Int = mRealCount + 2

    //扩展之后的倒数第3条数据
    private fun exThreeLastPos(): Int = mRealCount + 1

    //正数第2条数据
    private fun exSecondPositive(): Int = 1


}