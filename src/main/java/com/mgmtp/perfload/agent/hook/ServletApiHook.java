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

import java.lang.reflect.Method;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.mgmtp.perfload.agent.AgentLogger;
import com.mgmtp.perfload.agent.annotations.Nullable;
import com.mgmtp.perfload.agent.util.ExecutionParams;

/**
 * Hook for extracting custom perfLoad headers from HTTP requeests.
 * 
 * @author rnaegele
 */
@Singleton
public class ServletApiHook extends AbstractHook {

	public static final String EXECUTION_ID_HEADER = "X-perfLoad-Execution-Id";
	public static final String OPERATION_HEADER = "X-perfLoad-Operation";
	public static final String REQUEST_ID_HEADER = "X-perfLoad-Request-Id";

	private final AgentLogger logger;
	private final Method getHeaderMethod;
	private final Provider<ExecutionParams> executionParamsProvider;

	@Inject
	ServletApiHook(final AgentLogger logger, @Nullable final Method getHeaderMethod,
			final Provider<ExecutionParams> executionParamsProvider) {
		this.logger = logger;
		this.getHeaderMethod = getHeaderMethod;
		this.executionParamsProvider = executionParamsProvider;
	}

	/**
	 * Retrieves custom perfLoad headers from the HTTP request and stores them in the current
	 * {@link ExecutionParams} object.
	 */
	@Override
	public void start(final Object source, final String fullyQualifiedMethodName, final Object[] args) {
		if (getHeaderMethod != null) {
			try {
				String executionId = (String) getHeaderMethod.invoke(args[0], EXECUTION_ID_HEADER);
				String operation = (String) getHeaderMethod.invoke(args[0], OPERATION_HEADER);
				String requestId = (String) getHeaderMethod.invoke(args[0], REQUEST_ID_HEADER);

				if (executionId != null && operation != null && requestId != null) {
					ExecutionParams execParams = executionParamsProvider.get();
					execParams.setExecutionId(UUID.fromString(executionId));
					execParams.setOperation(operation);
					execParams.setRequestId(UUID.fromString(requestId));
				}
			} catch (Exception ex) {
				logger.writeln(ex.getMessage(), ex);
			}
		}
	}

	/**
	 * Clears the current {@link ExecutionParams} object.
	 */
	@Override
	public void stop(final Object source, final Throwable throwable, final String fullyQualifiedMethodName, final Object[] args) {
		executionParamsProvider.get().clear();
	}
}
