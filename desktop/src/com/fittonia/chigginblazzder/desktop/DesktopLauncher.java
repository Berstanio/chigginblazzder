package com.fittonia.chigginblazzder.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.fittonia.chigginblazzder.MainApp;

/** Launches the desktop (LWJGL) application. */
public class DesktopLauncher {
	public static void main(String[] args) {
		createApplication();
	}

	private static LwjglApplication createApplication() {
		return new LwjglApplication(new MainApp(true), getDefaultConfiguration());
	}

	private static LwjglApplicationConfiguration getDefaultConfiguration() {
		LwjglApplicationConfiguration configuration = new LwjglApplicationConfiguration();
		configuration.title     = "ChigginBlazzder";
		configuration.width     = 800;
		configuration.height    = 600;
		configuration.samples   = 1;
//		configuration.resizable = false;

		return configuration;
	}
}