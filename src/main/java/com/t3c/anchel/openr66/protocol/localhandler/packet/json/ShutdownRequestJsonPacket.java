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
 * Shutdown current request JSON packet
 * 
 * @author "Frederic Bregier"
 *
 */
public class ShutdownRequestJsonPacket extends JsonPacket {

    protected int rank = -1;

    /**
     * @return the rank
     */
    public int getRank() {
        return rank;
    }

    /**
     * @param rank
     *            the rank to set
     */
    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setRequestUserPacket() {
        super.setRequestUserPacket(LocalPacketFactory.SHUTDOWNPACKET);
    }
}
