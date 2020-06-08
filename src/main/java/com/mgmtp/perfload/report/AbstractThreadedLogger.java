package com.mgmtp.perfload.report;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractThreadedLogger implements ResultLogger {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractThreadedLogger.class);
	public static final int N_THREADS = 50;
	private final ExecutorService threadPool = Executors.newFixedThreadPool(N_THREADS);
	private final Semaphore sem = new Semaphore(N_THREADS);

	protected AbstractThreadedLogger() {
	}

	public void log(String operation, String errorMessage, long timestamp, StopWatch ti1, StopWatch ti2, String type, String uri, String uriAlias, UUID executionId, UUID requestId, Object... extraArgs) throws InterruptedException {
		sem.acquire();
		LOG.debug("Lock acquired: {}", sem.availablePermits());
		threadPool.submit(() -> {
			try {
				if (threadPool.isShutdown()) {
					throw new InterruptedException("Logger is shutting down. Unable to log!");
				}
				processLog(new ResultObject(operation, errorMessage, timestamp, ti1, ti2, type, uri, uriAlias, executionId, requestId, extraArgs));
			} finally {
				sem.release();
				LOG.debug("Lock released: {}", sem.availablePermits());
			}
			return null;
		});

	}

	protected abstract void processLog(ResultObject r);

	public void flush() throws InterruptedException {
		LOG.debug("Flushing");
		ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
		ex.scheduleAtFixedRate(() -> {
			LOG.debug("Waiting for {} threads", N_THREADS - sem.availablePermits());
			if (sem.availablePermits() >= N_THREADS) {
				ex.shutdown();
			}
		}, 100, 30, TimeUnit.MILLISECONDS);
		ex.awaitTermination(30, TimeUnit.SECONDS);
		LOG.debug("Flushed");
	}

	public void shutdown() {
		awaitTerminationAfterShutdown(threadPool);
	}

	public void awaitTerminationAfterShutdown(ExecutorService threadPool) {
		threadPool.shutdown();
		try {
			if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
				threadPool.shutdownNow();
			}
		} catch (InterruptedException ex) {
			threadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	protected static class ResultObject {
		private final String operation;
		private final String errorMessage;
		private final long timestamp;
		private final StopWatch ti1;
		private final StopWatch ti2;
		private final String type;
		private final String uri;
		private final String uriAlias;
		private final UUID executionId;
		private final UUID requestId;
		private final Object[] extraArgs;

		public ResultObject(String operation, String errorMessage, long timestamp, StopWatch ti1, StopWatch ti2, String type, String uri, String uriAlias, UUID executionId, UUID requestId, Object[] extraArgs) {
			super();
			this.operation = operation;
			this.errorMessage = errorMessage;
			this.timestamp = timestamp;
			this.ti1 = ti1;
			this.ti2 = ti2;
			this.type = type;
			this.uri = uri;
			this.uriAlias = uriAlias;
			this.executionId = executionId;
			this.requestId = requestId;
			this.extraArgs = extraArgs;
		}

		public Object[] getExtraArgs() {
			return extraArgs;
		}

		public UUID getRequestId() {
			return requestId;
		}

		public UUID getExecutionId() {
			return executionId;
		}

		public String getUriAlias() {
			return uriAlias;
		}

		public String getUri() {
			return uri;
		}

		public String getType() {
			return type;
		}

		public StopWatch getTi2() {
			return ti2;
		}

		public StopWatch getTi1() {
			return ti1;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public String getErrorMessage() {
			return errorMessage;
		}

		public String getOperation() {
			return operation;
		}
	}
}
