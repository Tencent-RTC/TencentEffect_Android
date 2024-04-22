package com.tencent.demo.camera.camerax;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class CameraXLifecycleOwner implements LifecycleOwner {
    private LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    public void doOnCreate() {
        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }

    public void doOnStart() {
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }

    public void doOnResume() {
        lifecycleRegistry.setCurrentState(Lifecycle.State.RESUMED);
    }

    public void doOnPause() {
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }

    public void doOnStop() {
        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
    }

    public void doOnDestroy() {
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }


}
