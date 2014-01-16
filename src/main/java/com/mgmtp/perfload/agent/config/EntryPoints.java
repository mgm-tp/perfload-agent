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

import java.util.List;

/**
 * @author rnaegele
 */
public class EntryPoints {

	private final List<String> servlets;
	private final List<String> filters;

	public EntryPoints(final List<String> servlets, final List<String> filters) {
		this.servlets = servlets;
		this.filters = filters;
	}

	public boolean hasFilter(final String fqcn) {
		return filters.contains(fqcn);
	}

	public boolean hasServlet(final String fqcn) {
		return servlets.contains(fqcn);
	}
}
