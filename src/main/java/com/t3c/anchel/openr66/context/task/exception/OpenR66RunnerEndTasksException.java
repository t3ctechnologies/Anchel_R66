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
package com.t3c.anchel.openr66.context.task.exception;

/**
 * Runner exception in end of tasks status
 * 
 * @author frederic bregier
 */
public class OpenR66RunnerEndTasksException extends OpenR66RunnerException {

    /**
     *
     */
    private static final long serialVersionUID = -5410909604328960778L;

    /**
    *
    */
    public OpenR66RunnerEndTasksException() {
        super();
    }

    /**
     * @param arg0
     * @param arg1
     */
    public OpenR66RunnerEndTasksException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    /**
     * @param arg0
     */
    public OpenR66RunnerEndTasksException(String arg0) {
        super(arg0);
    }

    /**
     * @param arg0
     */
    public OpenR66RunnerEndTasksException(Throwable arg0) {
        super(arg0);
    }

}
