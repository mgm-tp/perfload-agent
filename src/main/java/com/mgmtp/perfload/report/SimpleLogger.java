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

import java.io.IOException;

/**
 * Interface for perfLoad's logger for measurings.
 *
 * @author rnaegele
 */
public interface SimpleLogger {

	/**
	 * Opens the logger.
	 */
	void open() throws IOException;

	/**
	 * Writes the output to logger.
	 */
	void writeln(final String output);

	/**
	 * Closes logger.
	 */
	void close();
}