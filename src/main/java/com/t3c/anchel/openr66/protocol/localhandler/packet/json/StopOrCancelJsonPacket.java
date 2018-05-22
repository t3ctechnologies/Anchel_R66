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

import com.t3c.anchel.openr66.database.DbConstant;
import com.t3c.anchel.openr66.protocol.localhandler.packet.LocalPacketFactory;

/**
 * Stop or Cancel one request JSON packet
 * 
 * @author "Frederic Bregier"
 *
 */
public class StopOrCancelJsonPacket extends JsonPacket {

    protected String requester;
    protected String requested;
    protected long specialid = DbConstant.ILLEGALVALUE;

    /**
     * @return the requester
     */
    public String getRequester() {
        return requester;
    }

    /**
     * @param requester
     *            the requester to set
     */
    public void setRequester(String requester) {
        this.requester = requester;
    }

    /**
     * @return the requested
     */
    public String getRequested() {
        return requested;
    }

    /**
     * @param requested
     *            the requested to set
     */
    public void setRequested(String requested) {
        this.requested = requested;
    }

    /**
     * @return the specialid
     */
    public long getSpecialid() {
        return specialid;
    }

    /**
     * @param specialid
     *            the specialid to set
     */
    public void setSpecialid(long specialid) {
        this.specialid = specialid;
    }

    public void setRequestUserPacket() {
        super.setRequestUserPacket(LocalPacketFactory.STOPPACKET);
    }

    public void setStop() {
        super.setRequestUserPacket(LocalPacketFactory.STOPPACKET);
    }

    public void setCancel() {
        super.setRequestUserPacket(LocalPacketFactory.CANCELPACKET);
    }
}