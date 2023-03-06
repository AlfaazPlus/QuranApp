package com.quranapp.android.frags;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;

import com.quranapp.android.activities.MainActivity;
import com.quranapp.android.interfaceUtils.ActivityResultStarter;
import com.quranapp.android.utils.receivers.NetworkStateReceiver;
import com.quranapp.android.utils.receivers.NetworkStateReceiver.NetworkStateReceiverListener;

public abstract class BaseFragment extends ResHelperFragment implements NetworkStateReceiverListener,
    ActivityResultStarter {
    private final ActivityResultLauncher<Intent> mActivityResultLauncher = activityResultHandler();
    private NetworkStateReceiver mNetworkReceiver;

    @Override
    public void onDestroy() {
        Context context = getContext();
        if (context != null && mNetworkReceiver != null) {
            mNetworkReceiver.removeListener(this);
            context.unregisterReceiver(mNetworkReceiver);
        }

        super.onDestroy();
    }

    @CallSuper
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (networkReceiverRegistrable() && getContext() != null) {
            mNetworkReceiver = new NetworkStateReceiver();
            mNetworkReceiver.addListener(this);
            getContext().registerReceiver(mNetworkReceiver, NetworkStateReceiver.getIntentFilter());
        }
    }

    public void restartMainActivity(Context ctx) {
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    protected boolean networkReceiverRegistrable() {
        return false;
    }

    public void hideSoftKeyboard(Activity actvt) {
        InputMethodManager imm = (InputMethodManager) actvt.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isAcceptingText()) {
            View currentFocus = actvt.getCurrentFocus();
            if (currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
    }

    public void launchActivity(Context ctx, Class<?> cls) {
        Intent intent = new Intent(ctx, cls);
        startActivity(intent);
    }

    public void runOnUIThread(Runnable runnable) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(runnable);
        }
    }

    @Override
    public void startActivity4Result(Intent intent, ActivityOptionsCompat options) {
        mActivityResultLauncher.launch(intent, options);
    }


    private ActivityResultLauncher<Intent> activityResultHandler() {
        return registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this::onActivityResult2);
    }

    protected void onActivityResult2(@NonNull ActivityResult result) {
    }

    @Override
    public void networkAvailable() {
    }

    @Override
    public void networkUnavailable() {
    }
}
