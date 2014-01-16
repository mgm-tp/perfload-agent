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
package com.mgmtp.perfload.agent;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.io.output.ProxyWriter;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.commons.lang3.time.DateFormatUtils;

import com.mgmtp.perfload.logging.SimpleLogger;

/**
 * Logger for the agent that logs to a file. A custom logger is used instead of a logging framework
 * in order to be independent of any possibly used logging frameworks in the application the agent
 * measures.
 * 
 * @author rnaegele
 */
public class AgentLogger implements SimpleLogger {

	private PrintWriter writer;
	private final File agentLogFile;

	public AgentLogger(final File agentLogfile) {
		this.agentLogFile = agentLogfile;
	}

	@Override
	public void open() throws IOException {
		writer = new PrintWriter(new DecoratingWriter(new FileWriterWithEncoding(agentLogFile, Charset.forName("UTF-8"))));
	}

	@Override
	public void writeln(final String output) {
		ensureOpen();
		writer.print(output);
	}

	public void writeln(final String output, final Throwable th) {
		ensureOpen();
		writer.print(output);
		th.printStackTrace(writer);
	}

	private void ensureOpen() {
		if (writer == null) {
			try {
				open();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		} else if (!agentLogFile.exists()) {
			// in case file was removed by Supervisor
			close();
			writer = null;
			ensureOpen();
		}
	}

	@Override
	public void close() {
		closeQuietly(writer);
	}

	/**
	 * Adds a prefix to each log line.
	 * 
	 * @author rnaegele
	 */
	static class DecoratingWriter extends ProxyWriter {

		public DecoratingWriter(final Writer proxy) {
			super(proxy);
		}

		@Override
		protected void beforeWrite(final int n) throws IOException {
			StrBuilder sb = new StrBuilder(43);
			sb.append(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(System.currentTimeMillis()));
			sb.append(" [perfLoad Agent] ");
			out.write(sb.toCharArray());
		}

		@Override
		protected void afterWrite(final int n) throws IOException {
			out.write(SystemUtils.LINE_SEPARATOR);
			out.flush();
		}
	}
}
