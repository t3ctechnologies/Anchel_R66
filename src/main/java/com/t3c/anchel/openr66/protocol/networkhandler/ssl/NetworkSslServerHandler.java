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
package com.t3c.anchel.openr66.protocol.networkhandler.ssl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import com.t3c.anchel.openr66.protocol.configuration.Configuration;
import com.t3c.anchel.openr66.protocol.networkhandler.NetworkServerHandler;
import com.t3c.anchel.openr66.protocol.networkhandler.NetworkTransaction;

/**
 * @author Frederic Bregier
 * 
 */
public class NetworkSslServerHandler extends NetworkServerHandler {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(NetworkSslServerHandler.class);

    /**
     * @param isServer
     */
    public NetworkSslServerHandler(boolean isServer) {
        super(isServer);
    }

    /**
     * 
     * @param channel
     * @return True if the SSL handshake is over and OK, else False
     */
    public static boolean isSslConnectedChannel(Channel channel) {
        return WaarpSslUtility.waitForHandshake(channel);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel networkChannel = ctx.channel();
        logger.debug("Add channel to ssl");
        WaarpSslUtility.addSslOpenedChannel(networkChannel);
        isSSL = true;
        // Check first if allowed
        if (NetworkTransaction.isBlacklisted(networkChannel)) {
            logger.warn("Connection refused since Partner is in BlackListed from "
                    + networkChannel.remoteAddress().toString());
            isBlackListed = true;
            if (Configuration.configuration.getR66Mib() != null) {
                Configuration.configuration.getR66Mib().notifyError(
                        "Black Listed connection temptative", "During Handshake");
            }
            // close immediately the connection
            WaarpSslUtility.closingSslChannel(networkChannel);
            return;
        }
        // Get the SslHandler in the current pipeline.
        // We added it in NetworkSslServerInitializer.
        final ChannelHandler handler = ctx.pipeline().first();
        if (handler instanceof SslHandler) {
            final SslHandler sslHandler = (SslHandler) handler;
            sslHandler.handshakeFuture().addListener(new GenericFutureListener<Future<? super Channel>>() {
                public void operationComplete(Future<? super Channel> future) throws Exception {
                    if (!future.isSuccess()) {
                        if (Configuration.configuration.getR66Mib() != null) {
                            Configuration.configuration.getR66Mib().notifyError(
                                    "SSL Connection Error", "During Handshake");
                        }
                    }
                }
            });
        } else {
            logger.error("SSL Not found");
        }
        super.channelActive(ctx);
    }
}
