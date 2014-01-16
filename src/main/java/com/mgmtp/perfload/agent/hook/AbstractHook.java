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

/**
 * Hook for timing methods.
 * 
 * @author rnaegele
 */
public abstract class AbstractHook implements Hook {

	/**
	 * Delegates to {@link #start(Object, String, Object[])} passing in an empty {@code args} array.
	 */
	@Override
	public void start(final Object source, final String fullyQualifiedMethodName) {
		start(source, fullyQualifiedMethodName, new Object[] {});
	}

	/**
	 * Delegates to {@link #stop(Object, Throwable, String, Object[])} passing in an empty
	 * {@code args} array.
	 */
	@Override
	public void stop(final Object source, final Throwable throwable, final String fullyQualifiedMethodName) {
		stop(source, throwable, fullyQualifiedMethodName, new Object[] {});
	}
}
