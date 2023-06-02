package  com.deepschneider.addressbook.drawer

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

import androidx.drawerlayout.widget.DrawerLayout

class Drawer : DrawerLayout {
    private var mInterceptTouchEventChildId = 0
    fun setInterceptTouchEventChildId(id: Int) {
        mInterceptTouchEventChildId = id
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mInterceptTouchEventChildId > 0) {
            val scroll: View? = findViewById(mInterceptTouchEventChildId)
            if (scroll != null) {
                val rect = Rect()
                scroll.getHitRect(rect)
                if (rect.contains(ev.x.toInt(), ev.y.toInt())) {
                    return false
                }
            }
        }
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (e: ArrayIndexOutOfBoundsException) {
            false
        }
    }
}