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
package com.t3c.anchel.openr66.protocol.utils;

import org.waarp.common.future.WaarpFuture;

import com.t3c.anchel.openr66.context.R66Result;
import com.t3c.anchel.openr66.database.data.DbTaskRunner;

/**
 * Future implementation
 * 
 * @author Frederic Bregier
 * 
 */
public class R66Future extends WaarpFuture {

    private R66Result result = null;
    /**
     * Used in some specific occasion, such as client submission in API mode
     */
    private DbTaskRunner runner = null;
    private long filesize = 0;

    /**
     *
     */
    public R66Future() {
    }

    /**
     * @param cancellable
     */
    public R66Future(boolean cancellable) {
        super(cancellable);
    }

    /**
     * @return the result
     */
    public R66Result getResult() {
        return result;
    }

    /**
     * @param result
     *            the result to set
     */
    public void setResult(R66Result result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "Future: " + isDone() + " " + isSuccess() + " " +
                (getCause() != null ? getCause().getMessage() : "no cause") +
                " " + (result != null ? result.toString() : "no result");
    }

    /**
     * @return the runner
     */
    public DbTaskRunner getRunner() {
        return runner;
    }

    /**
     * @param runner the runner to set
     */
    public void setRunner(DbTaskRunner runner) {
        this.runner = runner;
    }

    /**
     * @return the filesize
     */
    public long getFilesize() {
        return filesize;
    }

    /**
     * @param filesize the filesize to set
     */
    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }
}
