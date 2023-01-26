/*
 * (c) Faisal Khan. Created on 4/10/2021.
 */

package com.quranapp.android.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.peacedesign.android.utils.Dimen;
import com.quranapp.android.R;
import com.quranapp.android.activities.base.BaseActivity;
import com.quranapp.android.adapters.ADPNotifs;
import com.quranapp.android.components.NotifModel;
import com.quranapp.android.databinding.ActivityNotifBinding;
import com.quranapp.android.utils.extended.GapedItemDecoration;
import com.quranapp.android.views.BoldHeader;

import java.util.ArrayList;

public class ActivityNotif extends BaseActivity {
    ActivityNotifBinding mBinding;

    @Override
    protected boolean shouldInflateAsynchronously() {
        return true;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_notif;
    }

    @Override
    protected void onActivityInflated(@NonNull View activityView, @Nullable Bundle savedInstanceState) {
        mBinding = ActivityNotifBinding.bind(activityView);

        initHeader(mBinding.header);
        initNotifs();
    }

    private void initHeader(BoldHeader header) {
        header.setCallback(this::finish);
        header.setBGColor(R.color.colorBGPage);
        header.setTitleText("Notifications");
    }

    private void initNotifs() {
        ArrayList<NotifModel> notifications = getNotifications();
        if (notifications == null || notifications.size() == 0) {
            mBinding.notifs.setVisibility(View.GONE);
            mBinding.noNotifs.setVisibility(View.VISIBLE);
            return;
        }

        ADPNotifs adapter = new ADPNotifs(notifications);

        mBinding.notifs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mBinding.notifs.setAdapter(adapter);
        mBinding.notifs.addItemDecoration(new GapedItemDecoration(Dimen.dp2px(this, 5)));

        mBinding.noNotifs.setVisibility(View.GONE);
        mBinding.notifs.setVisibility(View.VISIBLE);
    }

    private ArrayList<NotifModel> getNotifications() {
        ArrayList<NotifModel> models = new ArrayList<>();
        NotifModel model = new NotifModel(R.drawable.dr_icon_notifications, "Friday Reminder!",
                "Today is friday, read suggested chapters. Surah Al-Kahf, Surah Yaseen & more", "2d ago");
        models.add(model);
        return models;
    }
}
