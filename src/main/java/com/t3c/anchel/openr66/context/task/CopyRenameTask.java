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
package com.t3c.anchel.openr66.context.task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import com.t3c.anchel.openr66.context.R66Session;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolSystemException;
import com.t3c.anchel.openr66.protocol.utils.FileUtils;

/**
 * Copy and Rename task
 * 
 * @author Frederic Bregier
 * 
 */
public class CopyRenameTask extends AbstractTask {
	/**
	 * Internal Logger
	 */
	private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(CopyRenameTask.class);

	/**
	 * @param argRule
	 * @param delay
	 * @param argTransfer
	 * @param session
	 */
	public CopyRenameTask(String argRule, int delay, String argTransfer, R66Session session) {
		super(TaskType.COPYRENAME, delay, argRule, argTransfer, session);
	}

	@Override
	public void run() {
		String finalname = argRule;
		finalname = getReplacedValue(finalname, argTransfer.split(" ")).replace('\\', '/');
		logger.info("Copy and Rename to " + finalname + " with " + argRule + ":" + argTransfer + " and {}", session);
		File from = session.getFile().getTrueFile();
		File to = new File(finalname);
		try {
			FileUtils.copy(from, to, false, false);
		} catch (OpenR66ProtocolSystemException e1) {
			logger.error("Copy and Rename to " + finalname + " with " + argRule + ":" + argTransfer + " and " + session,
					e1);
			futureCompletion.setFailure(new OpenR66ProtocolSystemException(e1));
			return;
		}
		futureCompletion.setSuccess();
		String data = "File downloaded successfuly.";
		File gatewayFile = new File(finalname.concat("_successFile"));
		OutputStream os;
		try {
			os = new FileOutputStream(gatewayFile);
			os.write(data.getBytes(), 0, data.length());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.error("Success File is created");
	}

}
