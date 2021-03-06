/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package com.t3c.anchel.openr66.protocol.localhandler.packet.json;

import com.t3c.anchel.openr66.protocol.localhandler.packet.LocalPacketFactory;

/**
 * File name or size changing Request JSON packet
 * 
 * @author "Frederic Bregier"
 *
 */
public class RequestJsonPacket extends JsonPacket {

    protected String filename;
    protected long filesize = -1;
    protected String fileInfo;

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename
     *            the filename to set
     */
    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * @return the filesize
     */
    public long getFilesize() {
        return filesize;
    }

    /**
     * @param filesize
     *            the filesize to set
     */
    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    /**
     * @return the fileInfo
     */
    public String getFileInfo() {
        return fileInfo;
    }

    /**
     * @param fileInfo the fileInfo to set
     */
    public void setFileInfo(String fileInfo) {
        this.fileInfo = fileInfo;
    }

    public void setRequestUserPacket() {
        super.setRequestUserPacket(LocalPacketFactory.REQUESTPACKET);
    }
}
