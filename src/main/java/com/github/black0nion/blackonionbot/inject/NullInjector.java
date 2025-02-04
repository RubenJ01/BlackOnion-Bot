package com.github.black0nion.blackonionbot.inject;

/**
 * The {@link #createInstance(Class, Class)} method is generic and thus {@code () -> null} can't be used as an {@link Injector}
 */
public class NullInjector implements Injector {
	public <T> T createInstance(Class<?> toInstantiate, Class<T> expectedType) {
		return null;
	}
}
