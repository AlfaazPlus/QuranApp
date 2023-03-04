package com.peacedesign.android.utils;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.peacedesign.android.utils.interfaceUtils.ObserverPro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static com.peacedesign.android.utils.interfaceUtils.ObserverPro.UpdateType.ADD;
import static com.peacedesign.android.utils.interfaceUtils.ObserverPro.UpdateType.CHANGE;
import static com.peacedesign.android.utils.interfaceUtils.ObserverPro.UpdateType.REMOVE;
import static com.peacedesign.android.utils.interfaceUtils.ObserverPro.UpdateType.SORT;

public class ArrayListPro<E> extends ArrayList<E> {
    private final List<ObserverPro<ArrayListPro<E>>> observers = new ArrayList<>();

    @NonNull
    @Override
    public E set(int index, @NonNull E element) {
        E set = super.set(index, element);
        notifyObservers(CHANGE);
        return set;
    }

    @Override
    public boolean add(@NonNull E e) {
        boolean add = super.add(e);
        notifyObservers(ADD);
        return add;
    }

    @Override
    public void add(int index, @NonNull E element) {
        super.add(index, element);
        notifyObservers(ADD);
    }

    @NonNull
    @Override
    public E remove(int index) {
        E remove = super.remove(index);
        notifyObservers(REMOVE);
        return remove;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        boolean remove = super.remove(o);
        notifyObservers(REMOVE);
        return remove;
    }

    @Override
    public void clear() {
        super.clear();
        notifyObservers(REMOVE);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        boolean addAll = super.addAll(c);
        notifyObservers(ADD);
        return addAll;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends E> c) {
        boolean addAll = super.addAll(index, c);
        notifyObservers(ADD);
        return addAll;
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
        notifyObservers(REMOVE);
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        boolean removeAll = super.removeAll(c);
        notifyObservers(REMOVE);
        return removeAll;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean removeIf(@NonNull Predicate<? super E> filter) {
        boolean removeIf = super.removeIf(filter);
        notifyObservers(REMOVE);
        return removeIf;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void replaceAll(@NonNull UnaryOperator<E> operator) {
        super.replaceAll(operator);
        notifyObservers(CHANGE);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void sort(@Nullable Comparator<? super E> c) {
        super.sort(c);
        notifyObservers(SORT);
    }

    private void notifyObservers(ObserverPro.UpdateType updateType) {
        for (ObserverPro<ArrayListPro<E>> observer : observers) {
            observer.onUpdate(this, updateType);
        }
    }

    public void addObserver(@NonNull ObserverPro<ArrayListPro<E>> observer) {
        observers.add(observer);
    }

    public void removeObserver(@NonNull ObserverPro<ArrayListPro<E>> observer) {
        observers.remove(observer);
    }

    @NonNull
    public ObserverPro<ArrayListPro<E>> getObserverAt(int index) {
        return observers.get(index);
    }

    public void removeObserverAt(int index) {
        observers.remove(index);
    }

    public int getObserversCount() {
        return observers.size();
    }
}
