/*
 * Copyright (c) 2013 mgm technology partners GmbH
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
package com.mgmtp.perfload.agent.annotations;

import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

/**
 * <p>
 * Guice scope which uses thread-local storage for Guice-managed objects. Using this scope
 * extensively may create potential memory leaks. Automatic clean-up is not possible, thus this
 * scope provides a {@link #cleanUp()} method for manual clean-up.
 * </p>
 * <p>
 * This scope must be bound to the {@link ThreadScoped} annotation in a Guice module. In order to
 * use the clean-up functionality ( {@link #cleanUp()}), it is necessary to bind the instance itself
 * so it can later be injected. Make sure you use the same instance in {@code bind(...)} and
 * {@code bindScope(...)} as shown in the following code sample:
 * </p>
 * <p>
 * 
 * <pre>
 * ThreadScope threadScope = new ThreadScope();
 * bindScope(ThreadScoped.class, threadScope);
 * bind(ThreadScope.class).toInstance(threadScope);
 * </pre>
 * 
 * </p>
 * 
 * @author rnaegele
 */
public final class ThreadScope implements Scope {

	private final ScopeCache scopeCache = new ScopeCache();

	@Override
	public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
		return new ThreadScopeProvider<T>(key, unscoped);
	}

	/**
	 * Clean-up method which deletes the scope cache for the current thread. Call this method after
	 * a thread is done in order to avoid memory leaks.
	 */
	public void cleanUp() {
		scopeCache.remove();
	}

	@Override
	public String toString() {
		return "ThreadScope";
	}

	private final class ThreadScopeProvider<T> implements Provider<T> {
		private final Key<T> key;
		private final Provider<T> unscoped;

		private ThreadScopeProvider(final Key<T> key, final Provider<T> unscoped) {
			this.key = key;
			this.unscoped = unscoped;
		}

		@Override
		public T get() {
			Map<Key<?>, Object> map = scopeCache.get();

			Object value = map.get(key);
			if (value == null) {
				value = map.get(key);
				if (value == null) {
					// no cached instance present, so we use the one
					// we get from the unscoped provider and add it to the cache
					value = unscoped.get();
					map.put(key, value);
				}
			}

			@SuppressWarnings("unchecked")
			// cast ok, because we know what we'd put in before
			T result = (T) value;
			return result;
		}

		@Override
		public String toString() {
			return String.format("%s[%s]", unscoped, ThreadScope.this);
		}
	}

	private static final class ScopeCache extends ThreadLocal<Map<Key<?>, Object>> {
		@Override
		protected Map<Key<?>, Object> initialValue() {
			return newHashMap();
		}
	}
}