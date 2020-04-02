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

import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

/**
 * A simple file logger.
 *
 * @author rnaegele
 */
public class InfluxDbTcpLogger implements SimpleLogger {

	InfluxDB influxDB;
	private final URI uri;

	/**
	 * @param uri the log file
	 */
	public InfluxDbTcpLogger(URI uri) {
		this.uri = uri;
	}

	/**
	 * Opens an auto-flushing {@link PrintWriter} to the output file using UTF-8 encoding.
	 */
	@Override
	public void open() {
		String userInfo = uri.getUserInfo();
		if (!userInfo.contains(":")) {
			try {
				userInfo = new String(Base64.getDecoder().decode(userInfo), StandardCharsets.UTF_8);
			} catch (IllegalArgumentException ignored) {
			}
		}
		String[] loginInfo = userInfo.split(":", 2);
		if (loginInfo.length > 1) {
			influxDB = InfluxDBFactory.connect(uri.getSchemeSpecificPart() + uri.getAuthority(), loginInfo[0], loginInfo[1]);
		} else {
			influxDB = InfluxDBFactory.connect(uri.getSchemeSpecificPart() + uri.getAuthority());
		}
		influxDB.setDatabase(uri.getPath());
		influxDB.enableBatch(BatchOptions.DEFAULTS);
	}

	/**
	 * Writes the output to the internal {@link PrintWriter} using
	 * {@link PrintWriter#println(String)} and {@link PrintWriter#flush()}.
	 */
	@Override
	public void writeln(final String output) {
		influxDB.write(output);
	}

	/**
	 * Closes the internal {@link PrintWriter}.
	 */
	@Override
	public void close() {
		influxDB.close();
	}
}