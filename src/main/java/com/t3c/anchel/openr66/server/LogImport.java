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

import java.io.File;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import com.t3c.anchel.AnchelSlf4jLoggerFactory;
import com.t3c.anchel.openr66.configuration.FileBasedConfiguration;
import com.t3c.anchel.openr66.database.DbConstant;
import com.t3c.anchel.openr66.database.data.DbTaskRunner;
import com.t3c.anchel.openr66.protocol.configuration.Configuration;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import com.t3c.anchel.openr66.protocol.utils.ChannelUtils;

/**
 * To import logs that were exported (one should not try to import on the same server to prevent
 * strange behavior).
 * 
 * @author "Frederic Bregier"
 * 
 */
public class LogImport {
    /**
     * Internal Logger
     */
    private static WaarpLogger logger;

    /**
     * @param args
     */
    public static void main(String[] args) {
        WaarpLoggerFactory.setDefaultFactory(new AnchelSlf4jLoggerFactory(null));
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(LogImport.class);
        }
        if (args.length < 2) {
            System.err
                    .println("Need configuration file and the logfile to import");
            System.exit(1);
        }
        try {
            if (!FileBasedConfiguration
                    .setConfigurationServerMinimalFromXml(Configuration.configuration, args[0])) {
                logger
                        .error("Needs a correct configuration file as first argument");
                if (DbConstant.admin != null) {
                    DbConstant.admin.close();
                }
                ChannelUtils.stopLogger();
                System.exit(1);
                return;
            }
            long time1 = System.currentTimeMillis();
            File logsFile = new File(args[1]);
            try {
                DbTaskRunner.loadXml(logsFile);
            } catch (OpenR66ProtocolBusinessException e) {
                logger.error("Cannot load the logs from " + logsFile.getAbsolutePath()
                        + " since: " + e.getMessage(), e);
                if (DbConstant.admin != null) {
                    DbConstant.admin.close();
                }
                ChannelUtils.stopLogger();
                System.exit(1);
                return;
            }
            long time2 = System.currentTimeMillis();
            long delay = time2 - time1;
            logger.warn("LogFile imported in " + delay + " ms");
        } finally {
            if (DbConstant.admin != null) {
                DbConstant.admin.close();
            }
            System.exit(0);
        }
    }
}
