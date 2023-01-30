/*
 * Copyright (c) Faisal Khan (https://github.com/faisalcodes)
 * Created on 4/4/2022.
 * All rights reserved.
 */

package com.quranapp.android.utils.univ;

import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * A class which maintains a list of transactions to occur when Context becomes available.
 */
public final class ActivityBuffer<R extends FragmentActivity> {
    public interface ActivityAvailableListener<R extends FragmentActivity> {
        /**
         * Executes when there's an available Context. Ideally, will it operate immediately.
         */
        void run(final R activity);
    }

    private R mActivity;
    private final List<ActivityAvailableListener<R>> mRunnables;

    /**
     * Constructor.
     */
    public ActivityBuffer() {
        // Initialize Member Variables.
        mRunnables = new ArrayList<>();
        mActivity = null;
    }

    /**
     * Executes the Runnable if there's an available Context. Otherwise, defers execution until it becomes available.
     */
    public void safely(final ActivityAvailableListener<R> listener) {
        // Synchronize along the current instance.
        synchronized (this) {
            // Do we have a context available?
            if (isContextAvailable()) {
                // Execute the Runnable along the Activity.
                mActivity.runOnUiThread(() -> listener.run(mActivity));
            } else {
                // Buffer the Runnable so that it's ready to receive a valid reference.
                getRunnables().add(listener);
            }
        }
    }

    /**
     * Called to inform the ActivityBuffer that there's an available Activity reference.
     */
    public void onActivityGained(final R activity) {
        // Synchronize along ourself.
        synchronized (this) {
            // Update the Activity reference.
            setActivity(activity);
            // Are there any Runnables awaiting execution?
            if (!getRunnables().isEmpty()) {
                // Iterate the Runnables.
                for (final ActivityAvailableListener<R> lRunnable : getRunnables()) {
                    // Execute the Runnable on the UI Thread.
                    activity.runOnUiThread(() -> {
                        // Execute the Runnable.
                        lRunnable.run(activity);
                    });
                }
                // Empty the Runnables.
                getRunnables().clear();
            }
        }
    }

    /**
     * Called to inform the ActivityBuffer that the Context has been lost.
     */
    public void onActivityLost() {
        // Synchronize along ourself.
        synchronized (this) {
            // Remove the Context reference.
            setActivity(null);
        }
    }

    /**
     * Defines whether there's a safe Context available for the ActivityBuffer.
     */
    public boolean isContextAvailable() {
        // Synchronize upon ourself.
        synchronized (this) {
            // Return the state of the Activity reference.
            return (mActivity != null);
        }
    }

    /* Getters and Setters. */
    private void setActivity(final R activity) {
        mActivity = activity;
    }

    private List<ActivityAvailableListener<R>> getRunnables() {
        return mRunnables;
    }
}