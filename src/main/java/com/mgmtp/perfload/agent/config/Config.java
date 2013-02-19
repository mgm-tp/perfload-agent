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
package com.mgmtp.perfload.agent.config;

import java.util.Map;

/**
 * @author rnaegele
 */
public class Config {

	private final EntryPoints entryPoints;
	private final Map<String, Map<String, MethodInstrumentations>> instrumentations;

	public Config(final EntryPoints entryPoints, final Map<String, Map<String, MethodInstrumentations>> instrumentations) {
		this.entryPoints = entryPoints;
		this.instrumentations = instrumentations;
	}

	/**
	 * @return the entryPoints
	 */
	public EntryPoints getEntryPoints() {
		return entryPoints;
	}

	/**
	 * Returns a map of class names mapped to method instrumentations by method names.
	 * 
	 * @return the instrumentations
	 */
	public Map<String, Map<String, MethodInstrumentations>> getInstrumentations() {
		return instrumentations;
	}
}
