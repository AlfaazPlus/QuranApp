package com.quranapp.android.widgets.radio

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.IdRes
import com.quranapp.android.R
import com.quranapp.android.widgets.compound.PeaceCompoundButtonGroup

class PeaceRadioGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), PeaceCompoundButtonGroup {
    private var initialOrientation = 0
    private var checkedId = 0
    private var protectFromCheckedChange = false

    /**
     * <p>Called before the checked radio button has changed. When the
     * selection is cleared, checkedId is -1.</p>
     *
     * First parameter is the group in which the checked radio button is to be changed
     * Second parameter is the unique identifier of the newly checked radio button
     * @return Return false to block check change.
     */
    var beforeCheckChangeListener: ((PeaceRadioGroup, Int) -> Boolean)? = null

    /**
     * <p>Called when the checked radio button has changed. When the
     * selection is cleared, checkedId is -1.</p>
     *
     * First parameter is the newly checked radio button
     * Second parameter is the unique identifier of the newly checked radio button
     */
    var onCheckChangedListener: ((PeaceRadioButton, Int) -> Unit)? = null

    private var passThroughListener = PassThroughHierarchyChangeListener()

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PeaceRadioGroup, defStyleAttr, 0)

        initialOrientation = a.getInt(R.styleable.PeaceRadioGroup_android_orientation, VERTICAL)
        checkedId = a.getResourceId(R.styleable.PeaceRadioGroup_android_checkedButton, NO_ID)

        a.recycle()

        super.setOrientation(initialOrientation)
        super.setOnHierarchyChangeListener(passThroughListener)
    }

    /**
     * {@inheritDoc}
     */
    override fun setOnHierarchyChangeListener(listener: OnHierarchyChangeListener) {
        passThroughListener.onHierarchyChangeListener = listener
    }

    /**
     * {@inheritDoc}
     */
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (checkedId == NO_ID) return

        protectFromCheckedChange = true
        setCheckedForView(checkedId, true)
        setCheckedId(checkedId)
        protectFromCheckedChange = false
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (child !is PeaceRadioButton) return

        child.setGroup(this)

        if (child.isChecked) {
            protectFromCheckedChange = true
            if (checkedId != NO_ID) setCheckedForView(checkedId, false)
            setCheckedId(child.id)
            protectFromCheckedChange = false
        }

        super.addView(child, index, params)
    }

    override fun clearCheck() {
        check(NO_ID)
    }

    /**
     * Check a button without invoking the [.mOnCheckedChangeListener] or [.mBeforeCheckedChangeListener].
     *
     * @param id Id of the [PeaceRadioButton] to be checked.
     */
    override fun checkSansInvocation(@IdRes id: Int) {
        protectFromCheckedChange = true
        check(id)
        protectFromCheckedChange = false
    }

    override fun checkAtPosition(position: Int) {
        val view = getChildAt(position) ?: return

        check(view.id)
    }

    override fun check(@IdRes id: Int) {
        if (id != NO_ID && id == checkedId) return

        if (protectFromCheckedChange || beforeCheckChangeListener == null) {
            checkInternal(id)
        } else if (beforeCheckChangeListener!!.invoke(this, id)) {
            checkInternal(id)
        }
    }

    private fun checkInternal(@IdRes id: Int) {
        setCheckedForView(checkedId, false) // uncheck previously checked button
        setCheckedForView(id, true) // check the new button
        setCheckedId(id)
    }

    private fun setCheckedForView(@IdRes id: Int, checked: Boolean) {
        if (id == NO_ID) return
        findViewById<PeaceRadioButton?>(id)?.isChecked = checked
    }

    private fun setCheckedId(@IdRes id: Int) {
        checkedId = id

        if (!protectFromCheckedChange) {
            onCheckChangedListener?.invoke(findViewById(checkedId), checkedId)
        }
    }

    fun getCheckedRadioId(): Int = checkedId

    /**
     *
     * A pass-through listener acts upon the events and dispatches them
     * to another listener. This allows the table layout to set its own internal
     * hierarchy change listener without preventing the user to setup his.
     */
    inner class PassThroughHierarchyChangeListener : OnHierarchyChangeListener {
        var onHierarchyChangeListener: OnHierarchyChangeListener? = null

        /**
         * {@inheritDoc}
         */
        override fun onChildViewAdded(parent: View, child: View) {
            if (parent === this@PeaceRadioGroup && child is PeaceRadioButton) {
                var id = child.getId()
                // generates an id if it's missing
                if (id == NO_ID) {
                    id = generateViewId()
                    child.setId(id)
                }
            }
            onHierarchyChangeListener?.onChildViewAdded(parent, child)
        }

        /**
         * {@inheritDoc}
         */
        override fun onChildViewRemoved(parent: View, child: View) {
            if (parent === this@PeaceRadioGroup && child is PeaceRadioButton) {
                onHierarchyChangeListener?.onChildViewRemoved(parent, child)
            }
        }
    }
}
