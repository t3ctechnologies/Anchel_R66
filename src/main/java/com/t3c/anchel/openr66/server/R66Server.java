/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package com.t3c.anchel.openr66.server;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import com.t3c.anchel.AnchelSlf4jLoggerFactory;
import com.t3c.anchel.openr66.configuration.FileBasedConfiguration;
import com.t3c.anchel.openr66.protocol.configuration.Configuration;
import com.t3c.anchel.openr66.protocol.configuration.Messages;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolPacketException;
import com.t3c.anchel.openr66.protocol.utils.R66ShutdownHook;

/**
 * R66Server startup main class
 * 
 * @author Frederic Bregier
 */
public class R66Server {
	private static WaarpLogger logger;

	/**
	 * @param args
	 *            as first argument the configuration file
	 * @throws OpenR66ProtocolPacketException
	 */
	public static void initR66Server(String[] args) throws OpenR66ProtocolPacketException {
		WaarpLoggerFactory.setDefaultFactory(new AnchelSlf4jLoggerFactory(null));
		logger = WaarpLoggerFactory.getLogger(R66Server.class);
		if (args.length < 1) {
			logger.error(Messages.getString("Configuration.NeedConfig")); //$NON-NLS-1$
			return;
		}
		try {
			R66ShutdownHook.registerMain(R66Server.class, args);
			if (initialize(args[0])) {
				logger.warn(Messages.getString("R66Server.ServerStart") + Configuration.configuration.getHOST_ID() //$NON-NLS-1$
						+ " : " + Configuration.configuration.toString());
				logger.info("************************************************************");
				logger.info("			WaarpR66Server Started Successfully				");
				logger.info("************************************************************");
				System.err.println(Messages.getString("R66Server.ServerStart") + Configuration.configuration.getHOST_ID()); //$NON-NLS-1$
				System.out.println("");
			} else {
				logger.error(Messages.getString("R66Server.CannotStart") + Configuration.configuration.getHOST_ID()); //$NON-NLS-1$
				logger.error("************************************************************");
				logger.error("				WaarpR66Server not Started				");
				logger.error("************************************************************");
				System.err.println(Messages.getString("R66Server.CannotStart") + Configuration.configuration.getHOST_ID()); //$NON-NLS-1$
				System.out.println("");
				System.exit(1);
			}
		} catch (Throwable e) {
			logger.error(Messages.getString("R66Server.StartError"), e); //$NON-NLS-1$
		}
	}

	public static boolean initialize(String config) {
		if (logger == null) {
			logger = WaarpLoggerFactory.getLogger(R66Server.class);
		}
		if (!FileBasedConfiguration.setConfigurationServerFromXml(Configuration.configuration, config)) {
			logger.error(Messages.getString("Configuration.NeedCorrectConfig")); //$NON-NLS-1$
			return false;
		}
		try {
			Configuration.configuration.serverStartup();
		} catch (Throwable e) {
			logger.error(Messages.getString("R66Server.StartError"), e); //$NON-NLS-1$
			R66ShutdownHook.terminate(false);
			return false;
		}
		return true;
	}
}
