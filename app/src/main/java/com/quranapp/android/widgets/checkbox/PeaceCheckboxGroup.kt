package com.quranapp.android.widgets.checkbox

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.IdRes

class PeaceCheckboxGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private var protectFromInvocation = false
    private var passThroughListener = PassThroughHierarchyChangeListener()
    var onItemCheckedChangeListener: ((PeaceCheckBox, Boolean) -> Unit)? = null
    private var checkedBoxes = ArrayList<PeaceCheckBox>()

    init {
        orientation = VERTICAL
        super.setOnHierarchyChangeListener(passThroughListener)
    }

    /**
     * {@inheritDoc}
     */
    override fun setOnHierarchyChangeListener(listener: OnHierarchyChangeListener) {
        passThroughListener.onHierarchyChangeListener = listener
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        if (child is PeaceCheckBox) {
            if (child.isChecked) {
                checkedChangedInternal(child, true)
            }
            child.onCheckChangedListener = { _, isChecked ->
                checkedChangedInternal(child, isChecked)

                if (!protectFromInvocation) {
                    onItemCheckedChangeListener?.invoke(child, isChecked)
                }
            }
        }
        super.addView(child, index, params)
    }

    private fun checkedChangedInternal(checkBox: PeaceCheckBox, isChecked: Boolean) {
        if (isChecked) {
            checkedBoxes.add(checkBox)
        } else {
            checkedBoxes.remove(checkBox)
        }
    }

    fun clearCheck(invokeListener: Boolean) {
        protectFromInvocation = !invokeListener

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is PeaceCheckBox) {
                child.isChecked = false
            }
        }

        protectFromInvocation = false
    }

    fun checkSansInvocation(@IdRes id: Int, checked: Boolean) {
        protectFromInvocation = true
        checkById(id, checked)
        protectFromInvocation = false
    }

    fun checkById(@IdRes id: Int, checked: Boolean) {
        setCheckedForView(id, checked)
    }

    fun checkByIndex(index: Int, checked: Boolean) {
        val checkBox = getChildAt(index)
        if (checkBox is PeaceCheckBox) {
            checkBox.isChecked = checked
        }
    }

    private fun setCheckedForView(@IdRes id: Int, checked: Boolean) {
        if (id == NO_ID) return
        findViewById<PeaceCheckBox?>(id)?.isChecked = checked
    }

    fun getCheckedBoxes(): List<PeaceCheckBox> {
        return checkedBoxes
    }

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
            if (parent === this@PeaceCheckboxGroup && child is PeaceCheckBox) {
                onHierarchyChangeListener?.onChildViewAdded(parent, child)
            }
        }

        /**
         * {@inheritDoc}
         */
        override fun onChildViewRemoved(parent: View, child: View) {
            if (parent === this@PeaceCheckboxGroup && child is PeaceCheckBox) {
                checkedChangedInternal(child, false)
                onHierarchyChangeListener?.onChildViewRemoved(parent, child)
            }
        }
    }
}
