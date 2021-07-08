/*
 * Copyright (c) 2014 mgm technology partners GmbH
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
package com.mgmtp.perfload.report;

import java.util.UUID;

import org.apache.commons.lang3.time.StopWatch;

/**
 * Interface for perfLoad's logger for measurings.
 *
 * @author rnaegele
 */
public interface ResultLogger {

	/**
	 * Writes the output to logger.
	 */
	void log(String operation, String errorMessage, long timestamp, StopWatch ti1, StopWatch ti2, String type, String uri,
		String uriAlias, UUID executionId, UUID requestId, Object... extraArgs) throws Exception;

}