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

/**
 * Interface for bytecode hooks.
 *
 * @author rnaegele
 */
public interface Hook {

	/**
	 * Starts the hook.
	 *
	 * @param source the runtime instance of the class the hook was inserted into
	 * @param fullyQualifiedMethodName the fully qualified name of the method the hook was inserted into
	 */
	void start(Object source, String fullyQualifiedMethodName);

	/**
	 * Starts the hook.
	 *
	 * @param source the runtime instance of the class the hook is inserted into
	 * @param fullyQualifiedMethodName the fully qualified name of the method the hook was inserted into
	 * @param args the method arguments, may be empty for a no-args method
	 */
	void start(Object source, String fullyQualifiedMethodName, Object[] args);

	/**
	 * Stops the hook.
	 *
	 * @param source the runtime instance of the class the hook is inserted into
	 * @param throwable a potential throwable that is on the method stack (may be non-null only if this is
	 * 	an option in the original byte code)
	 * @param fullyQualifiedMethodName the fully qualified name of the method the hook was inserted into
	 */
	void stop(Object source, Throwable throwable, String fullyQualifiedMethodName);

	/**
	 * Stops the hook.
	 *
	 * @param source the runtime instance of the class the hook is inserted into
	 * @param throwable a potential throwable that is on the method stack (may be non-null only if this is
	 * 	an option in the original byte code)
	 * @param fullyQualifiedMethodName the fully qualified name of the method the hook was inserted into
	 * @param args the method arguments, may be empty for a no-args method
	 */
	void stop(Object source, Throwable throwable, String fullyQualifiedMethodName, Object[] args);
}
