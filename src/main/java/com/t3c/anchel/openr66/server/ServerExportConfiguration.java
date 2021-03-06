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

import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import com.t3c.anchel.AnchelSlf4jLoggerFactory;
import com.t3c.anchel.openr66.configuration.FileBasedConfiguration;
import com.t3c.anchel.openr66.database.DbConstant;
import com.t3c.anchel.openr66.database.data.DbTaskRunner;
import com.t3c.anchel.openr66.protocol.configuration.Configuration;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import com.t3c.anchel.openr66.protocol.localhandler.ServerActions;
import com.t3c.anchel.openr66.protocol.utils.ChannelUtils;

/**
 * Server local configuration export to files
 * 
 * @author Frederic Bregier
 * 
 */
public class ServerExportConfiguration {
    /**
     * Internal Logger
     */
    private static WaarpLogger logger;

    /**
     * 
     * @param args
     *            as configuration file and the directory where to export
     */
    public static void main(String[] args) {
        WaarpLoggerFactory.setDefaultFactory(new AnchelSlf4jLoggerFactory(null));
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(ServerExportConfiguration.class);
        }
        if (args.length < 2) {
            System.err
                    .println("Need configuration file and the directory where to export");
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
            String directory = args[1];
            String hostname = Configuration.configuration.getHOST_ID();
            logger.info("Start of Export");
            File dir = new File(directory);
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }
            String[] filenames = ServerActions.staticConfigExport(DbConstant.admin.getSession(), dir.getAbsolutePath(),
                    true, true, true, true, true);
            for (String string : filenames) {
                if (string != null) {
                    logger.info("Export: " + string);
                }
            }
            String filename = dir.getAbsolutePath() + File.separator + hostname
                    + "_Runners.run.xml";
            try {
                DbTaskRunner.writeXMLWriter(filename);
            } catch (WaarpDatabaseNoConnectionException e1) {
                logger.error("Error", e1);
                DbConstant.admin.close();
                ChannelUtils.stopLogger();
                System.exit(2);
            } catch (WaarpDatabaseSqlException e1) {
                logger.error("Error", e1);
                DbConstant.admin.close();
                ChannelUtils.stopLogger();
                System.exit(2);
            } catch (OpenR66ProtocolBusinessException e1) {
                logger.error("Error", e1);
                DbConstant.admin.close();
                ChannelUtils.stopLogger();
                System.exit(2);
            }
            logger.info("End of Export");
        } finally {
            if (DbConstant.admin != null) {
                DbConstant.admin.close();
            }
            System.exit(0);
        }
    }

}
