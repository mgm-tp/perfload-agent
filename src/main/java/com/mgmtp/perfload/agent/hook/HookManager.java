/*
 * Copyright (c) 2013-2014 mgm technology partners GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mgmtp.perfload.agent.hook;

import com.google.inject.Key;
import com.mgmtp.perfload.agent.InjectorHolder;
import com.mgmtp.perfload.agent.annotations.Measuring;
import com.mgmtp.perfload.agent.annotations.ServletApi;

/**
 * Contains static helper methods that can easily be woven into a method's byte code using ASM.
 * 
 * @author rnaegele
 */
public class HookManager {

	private static final Key<Hook> MEASURING_KEY = Key.get(Hook.class, Measuring.class);
	private static final Key<Hook> SERVLET_API_KEY = Key.get(Hook.class, ServletApi.class);

	public static void enterMeasuringHook(final Object source, final String fullyQualifiedMethodName) {
		getHook(MEASURING_KEY).start(source, fullyQualifiedMethodName);
	}

	public static void enterMeasuringHook(final Object source, final String fullyQualifiedMethodName, final Object[] args) {
		getHook(MEASURING_KEY).start(source, fullyQualifiedMethodName, args);
	}

	public static void exitMeasuringHook(final Object source, final Throwable throwable, final String fullyQualifiedMethodName) {
		getHook(MEASURING_KEY).stop(source, throwable, fullyQualifiedMethodName);
	}

	public static void exitMeasuringHook(final Object source, final Throwable throwable, final String fullyQualifiedMethodName,
			final Object[] args) {
		getHook(MEASURING_KEY).stop(source, throwable, fullyQualifiedMethodName, args);
	}

	public static void enterServletApiHook(final Object source, final Object[] args) {
		getHook(SERVLET_API_KEY).start(source, null, args);
	}

	public static void exitServletApiHook() {
		getHook(SERVLET_API_KEY).stop(null, null, null);
	}

	private static Hook getHook(final Key<Hook> key) {
		return InjectorHolder.INSTANCE.getInjector().getInstance(key);
	}
}
