package com.t3c.anchel;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLoggerFactory;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLogger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class AnchelSlf4jLoggerFactory extends WaarpLoggerFactory {
	

	static final String ROOT = Logger.ROOT_LOGGER_NAME;

	public AnchelSlf4jLoggerFactory(WaarpLogLevel level) {
		super(level);
		seLevelSpecific(currentLevel);
	}

	@Override
	protected void seLevelSpecific(final WaarpLogLevel level) {
		LoggerContext context = null;
		if (!(LoggerFactory.getILoggerFactory()
				.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) instanceof ch.qos.logback.classic.Logger)) {
			context = new LoggerContext();
		} else {
			context = (LoggerContext) LoggerFactory.getILoggerFactory();
		}
		final Logger logger = context.getLogger(ROOT);
		switch (level) {
		case TRACE:
			logger.setLevel(Level.TRACE);
			break;
		case DEBUG:
			logger.setLevel(Level.DEBUG);
			break;
		case INFO:
			logger.setLevel(Level.INFO);
			break;
		case WARN:
			logger.setLevel(Level.WARN);
			break;
		case ERROR:
			logger.setLevel(Level.ERROR);
			break;
		default:
			logger.setLevel(Level.WARN);
			break;
		}
	}

	@Override
	public WaarpLogger newInstance(final String name) {
		LoggerContext context = null;
		if (!(LoggerFactory.getILoggerFactory()
				.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) instanceof ch.qos.logback.classic.Logger)) {
			context = new LoggerContext();
		} else {
			context = (LoggerContext) LoggerFactory.getILoggerFactory();
		}
		final Logger logger = context.getLogger(name);
		return new WaarpSlf4JLogger(logger);
	}

	AnchelSlf4jLoggerFactory(final boolean failIfNOP) {
		super(null);
		assert failIfNOP; // Should be always called with true.

		// SFL4J writes it error messages to System.err. Capture them so that
		// the user does not see such a message on
		// the console during automatic detection.
		final StringBuffer buf = new StringBuffer();
		final PrintStream err = System.err;
		try {
			System.setErr(new PrintStream(new OutputStream() {
				@Override
				public void write(final int b) {
					buf.append((char) b);
				}
			}, true, "US-ASCII"));
		} catch (final UnsupportedEncodingException e) {
			throw new Error(e);
		}

		try {
			if (LoggerFactory.getILoggerFactory() instanceof NOPLoggerFactory) {
				throw new NoClassDefFoundError(buf.toString());
			} else {
				err.print(buf.toString());
				err.flush();
			}
		} finally {
			System.setErr(err);
			seLevelSpecific(currentLevel);
		}
	}

	@Override
	protected WaarpLogLevel getLevelSpecific() {
		LoggerContext context = null;
		if (!(LoggerFactory.getILoggerFactory()
				.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME) instanceof ch.qos.logback.classic.Logger)) {
			context = new LoggerContext();
		} else {
			context = (LoggerContext) LoggerFactory.getILoggerFactory();
		}
		final Logger logger = context.getLogger(ROOT);
		if (logger.isTraceEnabled()) {
			return WaarpLogLevel.TRACE;
		} else if (logger.isDebugEnabled()) {
			return WaarpLogLevel.DEBUG;
		} else if (logger.isInfoEnabled()) {
			return WaarpLogLevel.INFO;
		} else if (logger.isWarnEnabled()) {
			return WaarpLogLevel.WARN;
		} else if (logger.isErrorEnabled()) {
			return WaarpLogLevel.ERROR;
		}
		return null;
	}
}
