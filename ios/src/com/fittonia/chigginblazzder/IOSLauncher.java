package com.fittonia.chigginblazzder;

import apple.uikit.c.UIKit;
import com.badlogic.gdx.backends.iosmoe.IOSApplication;
import com.badlogic.gdx.backends.iosmoe.IOSApplicationConfiguration;
import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.objc.ObjCAutoreleasePool;

@RegisterOnStartup
public class IOSLauncher extends IOSApplication.Delegate {
    protected IOSLauncher (Pointer peer) {
        super(peer);
    }

    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.orientationLandscape = true;
        config.orientationPortrait = false;
        config.allowIpod = true;
        return new IOSApplication(new MainApp(false), config);
    }

    public static void main(String[] argv) throws Exception {
        ObjCAutoreleasePool pool = new ObjCAutoreleasePool();
        UIKit.UIApplicationMain(0, null, null, IOSLauncher.class.getName());
        pool.close();
    }
}