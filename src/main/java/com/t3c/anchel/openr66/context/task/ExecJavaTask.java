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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpThreadFactory;

import com.t3c.anchel.openr66.context.ErrorCode;
import com.t3c.anchel.openr66.context.R66Result;
import com.t3c.anchel.openr66.context.R66Session;

/**
 * Execute a Java command through Class.forName call
 * 
 * 
 * @author Frederic Bregier
 * 
 */
public class ExecJavaTask extends AbstractTask {
    protected boolean businessRequest = false;

    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(ExecJavaTask.class);

    /**
     * @param argRule
     * @param delay
     * @param argTransfer
     * @param session
     */
    public ExecJavaTask(String argRule, int delay, String argTransfer,
            R66Session session) {
        super(TaskType.EXECJAVA, delay, argRule, argTransfer, session);
    }

    /**
     * Set the type
     * 
     * @param businessRequest
     */
    public void setBusinessRequest(boolean businessRequest) {
        this.businessRequest = businessRequest;
    }

    @Override
    public void run() {
        /*
         * First apply all replacements and format to argRule from context and argTransfer. Will
         * call exec (from first element of resulting string) with arguments as the following value
         * from the replacements. Return 0 if OK, else 1 for a warning else as an error. No change
         * should be done in the FILENAME
         */
        String finalname = argRule;
        if (argTransfer != null) {
            finalname = getReplacedValue(finalname, argTransfer.split(" "));
        }
        // First get the Class Name
        String[] args = finalname.split(" ");
        String className = args[0];
        boolean isSpooled = className.equals(SpooledInformTask.class.getName());
        if (isSpooled) {
            logger.debug("Exec with " + className + ":" + argTransfer + " and {}",
                    session);
        } else {
            logger.debug("Exec with " + argRule + ":" + argTransfer + " and {}",
                    session);
        }
        R66Runnable runnable = null;
        try {
            runnable = (R66Runnable) Class.forName(className).newInstance();
        } catch (Exception e) {
            logger.error("ExecJava command is not available: " + className, e);
            R66Result result = new R66Result(session, false,
                    ErrorCode.CommandNotFound, session.getRunner());
            futureCompletion.setResult(result);
            futureCompletion.cancel();
            return;
        }
        if (businessRequest) {
            boolean istovalidate = Boolean.parseBoolean(args[args.length - 1]);
            runnable.setArgs(this.session, this.waitForValidation, this.useLocalExec,
                    this.delay, className, finalname.substring(finalname.indexOf(' ') + 1, finalname.lastIndexOf(' ')),
                    businessRequest, istovalidate);
        } else {
            runnable.setArgs(this.session, this.waitForValidation, this.useLocalExec,
                    this.delay, className, finalname.substring(className.length() + 1), businessRequest, false);
        }
        logger.debug(className + " " + runnable.getClass().getName());
        if (!waitForValidation) {
            // Do not wait for validation
            futureCompletion.setSuccess();
            logger.info("Exec will start but no WAIT with {}", runnable);
        }
        int status = -1;
        if (waitForValidation && delay <= 100) {
            runnable.run();
            status = runnable.getFinalStatus();
        } else {
            ExecutorService executorService = Executors.newFixedThreadPool(1, new WaarpThreadFactory("JavaExecutor"));
            executorService.execute(runnable);
            try {
                Thread.yield();
                executorService.shutdown();
                if (waitForValidation) {
                    if (delay > 100) {
                        if (!executorService.awaitTermination(delay, TimeUnit.MILLISECONDS)) {
                            executorService.shutdownNow();
                            logger.error("Exec is in Time Out");
                            status = -1;
                        } else {
                            status = runnable.getFinalStatus();
                        }
                    } else {
                        while (!executorService.awaitTermination(30, TimeUnit.SECONDS))
                            ;
                        status = runnable.getFinalStatus();
                    }
                } else {
                    while (!executorService.awaitTermination(30, TimeUnit.SECONDS))
                        ;
                    status = runnable.getFinalStatus();
                }
            } catch (InterruptedException e) {
                logger.error("Status: " + e.getMessage() + " \t Exec in error with " +
                        runnable);
                if (waitForValidation) {
                    futureCompletion.cancel();
                }
                return;
            }
        }
        if (status == 0) {
            if (waitForValidation) {
                R66Result result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                result.setOther(runnable.toString());
                futureCompletion.setResult(result);
                futureCompletion.setSuccess();
            }
            if (isSpooled) {
                logger.info("Exec OK with {}", className);
            } else {
                logger.info("Exec OK with {}", runnable);
            }
        } else if (status == 1) {
            logger.warn("Exec in warning with " + runnable);
            if (waitForValidation) {
                R66Result result = new R66Result(session, true, ErrorCode.CompleteOk, null);
                result.setOther(runnable.toString());
                futureCompletion.setResult(result);
                futureCompletion.setSuccess();
            }
        } else {
            logger.error("Status: " + status + " Exec in error with " +
                    runnable);
            if (waitForValidation) {
                futureCompletion.cancel();
            }
        }
    }

}
