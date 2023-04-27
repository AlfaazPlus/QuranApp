/*
 * Created by Faisal Khan on (c) 26/8/2021.
 */

package com.quranapp.android.utils.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quranapp.android.views.recitation.RecitationPlayer;


public class RecitationPlayerReceiver extends BroadcastReceiver {
    public static final String ACTION_STOP = "RecitationPlayer.action.stop";
    public static final String ACTION_PLAY_CONTROL = "RecitationPlayer.action.play_control";
    public static final String ACTION_PLAY = "RecitationPlayer.action.play";
    public static final String ACTION_PAUSE = "RecitationPlayer.action.pause";
    public static final String ACTION_PREVIOUS_VERSE = "RecitationPlayer.action.previous_verse";
    public static final String ACTION_NEXT_VERSE = "RecitationPlayer.action.next_verse";
    private RecitationPlayer mRecitationPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mRecitationPlayer == null || intent == null) {
            return;
        }

        switch (intent.getAction()) {
            case ACTION_PLAY_CONTROL:
                //                mRecitationPlayer.playControl();
                break;
            case ACTION_PREVIOUS_VERSE:
                //                mRecitationPlayer.recitePreviousVerse();
                break;
            case ACTION_NEXT_VERSE:
                //                mRecitationPlayer.reciteNextVerse();
                break;
        }
    }

    public void setPlayer(RecitationPlayer recitationPlayer) {
        mRecitationPlayer = recitationPlayer;
    }
}