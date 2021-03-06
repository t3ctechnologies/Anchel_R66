/*
 * Copyright 2009 Red Hat, Inc.
 * 
 * Red Hat licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.t3c.anchel.openr66.protocol.http.rest.test;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpMethod;

import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.json.JsonHandler;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RootOptionsRestMethodHandler;
import org.waarp.gateway.kernel.rest.client.HttpRestClientSimpleResponseHandler;
import org.waarp.gateway.kernel.rest.client.RestFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.t3c.anchel.openr66.database.data.DbHostAuth;
import com.t3c.anchel.openr66.database.data.DbHostConfiguration;
import com.t3c.anchel.openr66.database.data.DbTaskRunner;
import com.t3c.anchel.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import com.t3c.anchel.openr66.protocol.http.rest.client.HttpRestR66ClientResponseHandler;
import com.t3c.anchel.openr66.protocol.http.rest.handler.HttpRestAbstractR66Handler.ACTIONS_TYPE;
import com.t3c.anchel.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.json.InformationJsonPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.json.JsonPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.json.RestartTransferJsonPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.json.StopOrCancelJsonPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket;

/**
 * Test Rest client response handler
 * 
 * Note that for testing, result is only the last "json" command, and therefore future is only
 * validated once all items are passed in a chain.
 * In normal condition, each step should produce: setting the RestArgument to the RestFuture and
 * validating (error or ok) to RestFuture.
 * 
 * @author Frederic Bregier
 */
public class HttpTestResponseHandler extends HttpRestR66ClientResponseHandler {
    /**
     * @param channel
     * @throws HttpInvalidAuthenticationException
     */

    @Override
    protected boolean afterError(Channel channel, RestArgument ra) {
        HttpTestRestR66Client.count.incrementAndGet();
        WaarpSslUtility.closingSslChannel(channel);
        return false;
    }

    @Override
    protected boolean afterDbGet(Channel channel, RestArgument ra) throws HttpInvalidAuthenticationException {
        HttpTestRestR66Client.count.incrementAndGet();
        // Update
        HttpTestRestR66Client.updateData(channel, ra);
        return true;
    }

    @Override
    protected boolean afterDbPost(Channel channel, RestArgument ra) throws HttpInvalidAuthenticationException {
        HttpTestRestR66Client.count.incrementAndGet();
        if (ra.getAnswer().path(DbHostAuth.Columns.ADMINROLE.name()).asBoolean()) {
            WaarpSslUtility.closingSslChannel(channel);
            return false;
        }
        // Select 1
        HttpTestRestR66Client.readData(channel, ra);
        return true;
    }

    @Override
    protected boolean afterDbPut(Channel channel, RestArgument ra) throws HttpInvalidAuthenticationException {
        HttpTestRestR66Client.count.incrementAndGet();
        if (ra.getAnswer().path(DbHostConfiguration.Columns.HOSTID.name()).asText().equals("hosta")) {
            WaarpSslUtility.closingSslChannel(channel);
            return false;
        }
        // Delete 1
        HttpTestRestR66Client.deleteData(channel, ra);
        return true;
    }

    @Override
    protected boolean afterDbDelete(Channel channel, RestArgument ra) {
        HttpTestRestR66Client.count.incrementAndGet();
        WaarpSslUtility.closingSslChannel(channel);
        return false;
    }

    @Override
    protected boolean afterDbGetMultiple(Channel channel, RestArgument ra) {
        HttpTestRestR66Client.count.incrementAndGet();
        WaarpSslUtility.closingSslChannel(channel);
        return false;
    }

    @Override
    protected boolean afterDbOptions(Channel channel, RestArgument ra) throws HttpInvalidAuthenticationException {
        HttpTestRestR66Client.count.incrementAndGet();
        boolean newMessage = false;
        AtomicInteger counter = null;
        RestFuture future = channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
        if (future.getOtherObject() == null) {
            counter = new AtomicInteger();
            future.setOtherObject(counter);
            JsonNode node = ra.getDetailedAllowOption();
            if (!node.isMissingNode()) {
                for (JsonNode jsonNode : node) {
                    Iterator<String> iterator = jsonNode.fieldNames();
                    while (iterator.hasNext()) {
                        String name = iterator.next();
                        if (!jsonNode.path(name).path(RestArgument.REST_FIELD.JSON_PATH.field).isMissingNode()) {
                            break;
                        }
                        if (name.equals(RootOptionsRestMethodHandler.ROOT)) {
                            continue;
                        }
                        counter.incrementAndGet();
                        HttpTestRestR66Client.options(channel, name);
                        newMessage = true;
                    }
                }
            }
        }
        if (!newMessage) {
            counter = (AtomicInteger) future.getOtherObject();
            newMessage = counter.decrementAndGet() > 0;
            if (!newMessage) {
                future.setOtherObject(null);
            }
        }
        if (!newMessage) {
            WaarpSslUtility.closingSslChannel(channel);
        }
        return newMessage;
    }

    @Override
    protected boolean action(Channel channel, RestArgument ra, ACTIONS_TYPE act) {
        HttpTestRestR66Client.count.incrementAndGet();
        boolean newMessage = false;
        switch (act) {
            case CreateTransfer: {
                // Continue with GetTransferInformation
                TransferRequestJsonPacket recv;
                try {
                    recv = (TransferRequestJsonPacket) JsonPacket.createFromBuffer(JsonHandler.writeAsString(ra
                            .getResults().get(0)));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return newMessage;
                }
                InformationJsonPacket node = new InformationJsonPacket(recv.getSpecialId(), false, recv.getRequested());
                HttpTestRestR66Client.action(channel, HttpMethod.GET, RESTHANDLERS.Control.uri, node);
                newMessage = true;
                break;
            }
            case ExecuteBusiness:
                // End
                break;
            case ExportConfig:
                // no Import in automatic test
                break;
            case GetBandwidth:
                // End
                break;
            case GetInformation:
                // End
                break;
            case GetLog:
                // End
                break;
            case GetTransferInformation: {
                // Continue with Stop in StopOrCancelTransfer
                ObjectNode answer = (ObjectNode) ra.getResults().get(0);
                StopOrCancelJsonPacket node = new StopOrCancelJsonPacket();
                node.setRequestUserPacket();
                node.setStop();
                node.setRequested(answer.path(DbTaskRunner.Columns.REQUESTED.name()).asText());
                node.setRequester(answer.path(DbTaskRunner.Columns.REQUESTER.name()).asText());
                node.setSpecialid(answer.path(DbTaskRunner.Columns.SPECIALID.name()).asLong());
                HttpTestRestR66Client.action(channel, HttpMethod.PUT, RESTHANDLERS.Control.uri, node);
                newMessage = true;
                break;
            }
            case ImportConfig:
                // End
                break;
            case OPTIONS:
                break;
            case RestartTransfer: {
                // Continue with delete transfer
                RestartTransferJsonPacket recv;
                try {
                    recv = (RestartTransferJsonPacket) JsonPacket.createFromBuffer(JsonHandler.writeAsString(ra
                            .getResults().get(0)));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return newMessage;
                }
                try {
                    HttpTestRestR66Client.deleteData(channel, recv.getRequested(), recv.getRequester(),
                            recv.getSpecialid());
                } catch (HttpInvalidAuthenticationException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                newMessage = true;
                break;
            }
            case SetBandwidth: {
                // Continue with GetBandwidth
                BandwidthJsonPacket recv;
                try {
                    recv = (BandwidthJsonPacket) JsonPacket.createFromBuffer(JsonHandler.writeAsString(ra.getResults()
                            .get(0)));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return newMessage;
                }
                recv.setSetter(false);
                HttpTestRestR66Client.action(channel, HttpMethod.GET, RESTHANDLERS.Bandwidth.uri, recv);
                newMessage = true;
                break;
            }
            case ShutdownOrBlock:
                // End
                break;
            case StopOrCancelTransfer: {
                // Continue with RestartTransfer
                StopOrCancelJsonPacket recv;
                try {
                    recv = (StopOrCancelJsonPacket) JsonPacket.createFromBuffer(JsonHandler.writeAsString(ra
                            .getResults().get(0)));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    return newMessage;
                }
                RestartTransferJsonPacket node = new RestartTransferJsonPacket();
                node.setRequestUserPacket();
                node.setRequested(recv.getRequested());
                node.setRequester(recv.getRequester());
                node.setSpecialid(recv.getSpecialid());
                HttpTestRestR66Client.action(channel, HttpMethod.PUT, RESTHANDLERS.Control.uri, node);
                newMessage = true;
                break;
            }
            case GetStatus:
                break;
            default:
                break;

        }
        if (!newMessage) {
            WaarpSslUtility.closingSslChannel(channel);
        }
        return newMessage;
    }
}
