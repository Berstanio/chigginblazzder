package com.fittonia.chigginblazzder.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.fittonia.chigginblazzder.MainApp;
import org.lwjgl.opengl.GL;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.LongPredicate;

/** Launches the desktop (LWJGL) application. */
public class DesktopLauncher {
	public static void main(String[] args) {
		ByteBuffer bb = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder());
		System.out.println("HII: " + SharedLibraryLoader.class.getResourceAsStream("/libgdxarm64.dylib"));
		//System.out.println(getFieldOffsetObject(bb.duplicate().order(bb.order()), bb));
		createApplication();
	}

	private static Lwjgl3Application createApplication() {
		return new Lwjgl3Application(new MainApp(true), getDefaultConfiguration());
	}

	private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
		Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
		configuration.setTitle("ChigginBlazzder");
		configuration.setWindowSizeLimits(800, 600, 800, 600);
		//configuration.samples   = 1;
//		configuration.resizable = false;

		return configuration;
	}

	private static long getFieldOffsetObject(Object container, Object value) {
		return getFieldOffset(container.getClass(), value.getClass(), offset -> getUnsafeInstance().getObject(container, offset) == value);
	}

	private static sun.misc.Unsafe getUnsafeInstance() {
		java.lang.reflect.Field[] fields = sun.misc.Unsafe.class.getDeclaredFields();

        /*
        Different runtimes use different names for the Unsafe singleton,
        so we cannot use .getDeclaredField and we scan instead. For example:

        Oracle: theUnsafe
        PERC : m_unsafe_instance
        Android: THE_ONE
        */
		for (java.lang.reflect.Field field : fields) {
			if (!field.getType().equals(sun.misc.Unsafe.class)) {
				continue;
			}

			int modifiers = field.getModifiers();
			if (!(java.lang.reflect.Modifier.isStatic(modifiers) && java.lang.reflect.Modifier.isFinal(modifiers))) {
				continue;
			}

			try {
				field.setAccessible(true);
				return (sun.misc.Unsafe)field.get(null);
			} catch (Exception ignored) {
			}
			break;
		}

		throw new UnsupportedOperationException("LWJGL requires sun.misc.Unsafe to be available.");
	}

	private static long getFieldOffset(Class<?> containerType, Class<?> fieldType, LongPredicate predicate) {
		Class<?> c = containerType;
		System.out.println("Trying to find type: " + fieldType.getName());

		while (c != Object.class) {
			System.out.println("Searching in class: " + c.getName());
			Field[] fields = c.getDeclaredFields();
			for (Field field : fields) {
				System.out.println("FOUND FIELD: " + field.getName());
				System.out.println("Has type: " + field.getType().getName());
				System.out.println("Is syntethic: " + field.isSynthetic());
				if (!field.getType().isAssignableFrom(fieldType) || Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
					continue;
				}
				System.out.println("FOUND FIELD APPLICABLE: " + field.getName());
				long offset = getUnsafeInstance().objectFieldOffset(field);
				if (predicate.test(offset)) {
					return offset;
				}
			}
			c = c.getSuperclass();
		}
		throw new UnsupportedOperationException("Failed to find field offset in class.");
	}
}