package com.redgeckotech.stockhawk.events;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;
import timber.log.Timber;

public class RxBus {

    private static final Subject<Object, Object> _bus = new SerializedSubject<>(PublishSubject.create());

    public void send(Object o) {
        _bus.onNext(o);
    }

    public Observable<Object> toObserverable() {
        return _bus;
    }

    public boolean hasObservers() {
        return _bus.hasObservers();
    }

    public <T> Subscription register(final Class<T> eventClass, Action1<T> onNext) {
        Timber.d("register called. %s", eventClass.getSimpleName());
        return _bus
                .filter(new Func1<Object, Boolean>() {
                    @Override
                    public Boolean call(Object obj) {
                        return obj.getClass().equals(eventClass);
                    }
                })
                .map(new Func1<Object, T>() {
                    @Override
                    public T call(Object o) {
                        return (T) o;
                    }
                })
                .subscribe(onNext);
    }
}