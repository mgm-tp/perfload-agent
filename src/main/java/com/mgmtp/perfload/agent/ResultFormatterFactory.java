package com.mgmtp.perfload.agent;

import com.mgmtp.perfload.report.ResultFormatter;

public interface ResultFormatterFactory {

	ResultFormatter createFormatter(String operation);
}
