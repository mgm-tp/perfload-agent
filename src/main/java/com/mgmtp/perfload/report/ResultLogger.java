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

import com.mgmtp.perfload.agent.StopWatch;

/**
 * Interface for logging test results.
 *
 * @author rnaegele
 */
public interface ResultLogger {

	/**
	 * Logs a test result.
	 *
	 * @param errorMessage the error message
	 * @param timestamp timestamp before taking time measurements
	 * @param ti1 a time interval representing a time measurement
	 * @param ti2 a time interval representing a time measurement
	 * @param type the type associated with this log message (e. g. the request type such as GET or
	 * 	POST for HTTP requests)
	 * @param uri the uri associated with this log message
	 * @param uriAlias the uriAlias associated with this log message
	 * @param executionId the execution id (all requests that are part of the same operation execution get
	 * 	the same execution id)
	 * @param requestId the request id (unique for each request)
	 * @param extraArgs additional application-specific arguments to be logged
	 */
	void logResult(String errorMessage, long timestamp, StopWatch ti1, StopWatch ti2, String type, String uri,
		String uriAlias, UUID executionId, UUID requestId, Object... extraArgs);

}