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
package com.t3c.anchel.openr66.protocol.localhandler.packet;

import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolPacketException;
import com.t3c.anchel.openr66.protocol.localhandler.LocalChannelReference;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Startup Message class
 * 
 * 1 localId (Integer): localId
 * 
 * @author frederic bregier
 */
public class StartupPacket extends AbstractLocalPacket {
    private final Integer localId;

    /**
     * @param headerLength
     * @param middleLength
     * @param endLength
     * @param buf
     * @return the new ValidPacket from buffer
     */
    public static StartupPacket createFromBuffer(int headerLength,
            int middleLength, int endLength, ByteBuf buf) {
        Integer newId = buf.readInt();
        return new StartupPacket(newId);
    }

    /**
     * @param newId
     */
    public StartupPacket(Integer newId) {
        localId = newId;
    }

    @Override
    public void createEnd(LocalChannelReference lcr) throws OpenR66ProtocolPacketException {
        end = Unpooled.EMPTY_BUFFER;
    }

    @Override
    public void createHeader(LocalChannelReference lcr) throws OpenR66ProtocolPacketException {
        header = Unpooled.buffer(4);
        header.writeInt(localId);
    }

    @Override
    public void createMiddle(LocalChannelReference lcr) throws OpenR66ProtocolPacketException {
        middle = Unpooled.EMPTY_BUFFER;
    }

    @Override
    public String toString() {
        return "StartupPacket: " + localId;
    }

    @Override
    public byte getType() {
        return LocalPacketFactory.STARTUPPACKET;
    }

    /**
     * @return the localId
     */
    public Integer getLocalId() {
        return localId;
    }

}
