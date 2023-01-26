package com.peacedesign.android.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peacedesign.android.utils.interfaceUtils.Position;
import com.peacedesign.android.utils.interfaceUtils.Selectable;

import java.io.Serializable;

public abstract class ComponentBase implements Selectable, Position, Serializable {
    private int mId = -1;
    private String mKey;
    private int mPosition = -1;
    private boolean mSelected;
    private boolean mEnabled = true;
    private transient Object object;


    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    @NonNull
    public String getKey() {
        return mKey;
    }

    public void setKey(@NonNull String key) {
        mKey = key;
    }

    @Override
    public int getPosition() {
        return mPosition;
    }

    @Override
    public void setPosition(int position) {
        mPosition = position;
    }

    @Override
    public boolean isSelected() {
        return mSelected;
    }

    @Override
    public void setSelected(boolean flag) {
        mSelected = flag;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(@Nullable Object object) {
        this.object = object;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean flag) {
        this.mEnabled = flag;
    }

    public void resetBools() {
        setSelected(false);
        setEnabled(true);
    }
}
