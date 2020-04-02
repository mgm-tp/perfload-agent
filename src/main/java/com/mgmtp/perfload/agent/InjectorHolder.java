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
package com.mgmtp.perfload.agent;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Enum singleton that holds the Guice injector.
 *
 * @author rnaegele
 */
public enum InjectorHolder {
	INSTANCE;

	private volatile Injector injector;

	public Injector createInjector(final Module module) {
		injector = Guice.createInjector(module);
		return injector;
	}

	/**
	 * @return the injector
	 */
	public Injector getInjector() {
		return injector;
	}
}
