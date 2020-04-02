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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple file logger.
 *
 * @author rnaegele
 */
public class InfluxDbTcpLogger implements SimpleLogger {

	private InfluxDB influxDb;
	private final URI uri;
	private final static Logger LOG = LoggerFactory.getLogger(InfluxDbTcpLogger.class);

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
		influxDb = Optional.of(new AuthInfo(uri))
			.filter(AuthInfo::isValid)
			.map(authInfo -> InfluxDBFactory.connect(getUrl(), authInfo.getUser(), authInfo.getPassword()))
			.orElse(InfluxDBFactory.connect(getUrl()))
			.setDatabase(getDatabase())
			.enableBatch(BatchOptions.DEFAULTS);
		LOG.info("Opened connection to InfluxDB URI: {}, DB: {}", getUrl(), getDatabase());
	}

	private String getUrl() {
		return uri.getScheme() + ":" + uri.getSchemeSpecificPart() + uri.getAuthority();
	}

	private String getDatabase() {
		return StringUtils.stripStart(uri.getPath(), "/");
	}

	/**
	 * Writes the output to the internal {@link PrintWriter} using
	 * {@link PrintWriter#println(String)} and {@link PrintWriter#flush()}.
	 */
	@Override
	public void writeln(final String output) {
		if (influxDb == null) {
			LOG.error("Connection to InfluxDB is not open.");
		} else {
			influxDb.write(output);
		}
	}

	/**
	 * Closes the internal {@link PrintWriter}.
	 */
	@Override
	public void close() {
		if (influxDb != null) {
			influxDb.close();
		}
	}

	private static class AuthInfo {

		private final String user;
		private final String password;

		public AuthInfo(URI uri) {
			String[] userPasswordPair = decodeAuthInfo(uri.getUserInfo()).split(":", 2);
			user = userPasswordPair.length > 0 ? userPasswordPair[0] : null;
			password = userPasswordPair.length > 1 ? userPasswordPair[1] : null;
		}

		private String decodeAuthInfo(String userInfo) {
			if (userInfo.contains(":")) {
				return userInfo;
			}
			try {
				return new String(Base64.getDecoder().decode(userInfo), StandardCharsets.UTF_8);
			} catch (IllegalArgumentException ignored) {
				return userInfo;
			}
		}

		public String getPassword() {
			return password;
		}

		public String getUser() {
			return user;
		}

		public boolean isValid() {
			return StringUtils.isNoneBlank(user, password);
		}
	}
}