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
package com.mgmtp.perfload.agent.config;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rnaegele
 */
public class Config {

	private static final Logger LOG = LoggerFactory.getLogger(Config.class);
	private final EntryPoints entryPoints;
	private final Map<String, Map<String, MethodInstrumentations>> instrumentations;

	public Config(final EntryPoints entryPoints, final Map<String, Map<String, MethodInstrumentations>> instrumentations) {
		this.entryPoints = entryPoints;
		this.instrumentations = instrumentations;
		LOG.debug("Created agent config: {}", this);
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

	@Override
	public String toString() {
		return String.format("Config:\nentryPoints:%s\ninstrumentations:%s", entryPoints,
			instrumentations.entrySet().stream()
				.flatMap(this::classToString)
				.collect(Collectors.joining("\n")));
	}

	private Stream<String> classToString(Map.Entry<String, Map<String, MethodInstrumentations>> classEntry) {
		String className = classEntry.getKey();
		Map<String, MethodInstrumentations> methods = classEntry.getValue();
		return methods.entrySet().stream().map(this::methodToString).map(s -> String.format("%s#%s", className, s));
	}

	private String methodToString(Map.Entry<String, MethodInstrumentations> methodEntry) {
		return String.format("%s: %s", methodEntry.getKey(), methodEntry.getValue());
	}
}
