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

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

/**
 * @author rnaegele
 */
public class MethodInstrumentations implements Iterable<List<String>> {

	private final String methodName;
	private final List<List<String>> argumentLists;

	public MethodInstrumentations(final String methodName, final List<List<String>> argumentLists) {
		this.methodName = methodName;
		this.argumentLists = argumentLists;
	}

	public boolean isEmpty() {
		return argumentLists.isEmpty();
	}

	/**
	 * @return the methodName
	 */
	public String getMethodName() {
		return methodName;
	}

	@Override
	public Iterator<List<String>> iterator() {
		return argumentLists.iterator();
	}

	@Override
	public String toString() {
		return Optional.ofNullable(argumentLists)
			.filter(CollectionUtils::isNotEmpty)
			.map(a ->
				a.stream()
					.map(args -> String.format("%s(%s)", methodName, args))
					.collect(Collectors.joining("; "))
			).orElse(String.format("%s(*)", methodName));
	}
}
