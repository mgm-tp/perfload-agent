package com.mgmtp.perfload.agent;

import com.mgmtp.perfload.report.ResultFormatter;

public interface ResultLoggerFactory {

	ResultFormatter createLogger(String operation);
}
