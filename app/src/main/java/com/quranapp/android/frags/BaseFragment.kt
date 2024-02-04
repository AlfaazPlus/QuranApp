package com.quranapp.android.frags

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.quranapp.android.interfaceUtils.ActivityResultStarter
import com.quranapp.android.utils.receivers.NetworkStateReceiver
import com.quranapp.android.utils.receivers.NetworkStateReceiver.Companion.intentFilter
import com.quranapp.android.utils.receivers.NetworkStateReceiver.NetworkStateReceiverListener

abstract class BaseFragment : Fragment(), NetworkStateReceiverListener, ActivityResultStarter {
    private val mActivityResultLauncher = activityResultHandler()
    private var mNetworkReceiver: NetworkStateReceiver? = null

    override fun onDestroy() {
        if (context != null && mNetworkReceiver != null) {
            mNetworkReceiver!!.removeListener(this)
            requireContext().unregisterReceiver(mNetworkReceiver)
        }
        super.onDestroy()
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (networkReceiverRegistrable() && context != null) {
            mNetworkReceiver = NetworkStateReceiver().apply {
                addListener(this@BaseFragment)
            }

            ContextCompat.registerReceiver(
                requireContext(),
                mNetworkReceiver,
                intentFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    fun getArgs(): Bundle = arguments ?: Bundle()

    fun restartMainActivity(ctx: Context) {
        // start a new Intent of the app
        startActivity(ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })

        // kill the current process
        Process.killProcess(Process.myPid())
    }

    protected open fun networkReceiverRegistrable(): Boolean {
        return false
    }

    fun hideSoftKeyboard(activity: Activity) {
        (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).let {
            if (it.isAcceptingText) {
                activity.currentFocus?.let { focus ->
                    it.hideSoftInputFromWindow(focus.windowToken, 0)
                }
            }
        }
    }

    fun launchActivity(ctx: Context?, cls: Class<*>?) {
        startActivity(Intent(ctx, cls))
    }

    fun runOnUIThread(runnable: Runnable?) {
        activity?.runOnUiThread(runnable)
    }

    override fun startActivity4Result(intent: Intent, options: ActivityOptionsCompat?) {
        mActivityResultLauncher.launch(intent, options)
    }

    private fun activityResultHandler(): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            onActivityResult2(
                result
            )
        }
    }

    protected open fun onActivityResult2(result: ActivityResult) {}
    override fun networkAvailable() {}
    override fun networkUnavailable() {}
}