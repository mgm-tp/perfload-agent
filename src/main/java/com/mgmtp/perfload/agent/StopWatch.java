package com.mgmtp.perfload.agent;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * Utility class for measuring time intervals.
 *
 * @author rnaeegele
 */
public class StopWatch {

	private Instant start;
	private Instant end;

	/**
	 * Starts measuring (i. e. sets the start time).
	 */
	public void start() {
		start = Instant.now();
	}

	/**
	 * Stops measuring (i. e. sets the end time).
	 */
	public void stop() {
		end = Instant.now();
	}

	/**
	 * @return The interval in milliseconds.
	 */
	public Duration duration() {
		if (end == null && start == null) {
			return null;
		}
		if (end == null) {
			stop();
		}
		return Duration.between(start, end);
	}

	/**
	 * Returns the interval with the format H:mm:ss:SSS using <br />
	 * {@code String.format("%d:%02d:%02d,%03d", hours, minutes, seconds, millis)}.
	 *
	 * @return the formatted interval
	 */
	public String format() {
		return DurationFormatUtils.formatDuration(duration().getNano() / 1000, "H:mm:ss.S");
	}

	@Override
	public String toString() {
		return "TimeInterval[" + format() + "]";
	}
}