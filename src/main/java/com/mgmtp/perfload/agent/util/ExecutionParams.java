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
package com.mgmtp.perfload.agent.util;

import java.util.UUID;

import com.mgmtp.perfload.agent.annotations.ThreadScoped;

/**
 * @author rnaegele
 */
@ThreadScoped
public class ExecutionParams {

	private UUID executionId;
	private UUID requestId;
	private String operation;

	public boolean isEmpty() {
		return executionId == null && requestId == null && operation == null;
	}

	public void clear() {
		executionId = null;
		requestId = null;
		operation = null;
	}

	/**
	 * @return the executionId
	 */
	public UUID getExecutionId() {
		return executionId;
	}

	/**
	 * @param executionId the executionId to set
	 */
	public void setExecutionId(final UUID executionId) {
		this.executionId = executionId;
	}

	/**
	 * @return the operation
	 */
	public String getOperation() {
		return operation;
	}

	/**
	 * @param operation the operation to set
	 */
	public void setOperation(final String operation) {
		this.operation = operation;
	}

	/**
	 * @return the requestId
	 */
	public UUID getRequestId() {
		return requestId;
	}

	/**
	 * @param requestId the requestId to set
	 */
	public void setRequestId(final UUID requestId) {
		this.requestId = requestId;
	}
}
