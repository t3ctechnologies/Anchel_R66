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
package com.t3c.anchel.openr66.protocol.localhandler;

import static com.t3c.anchel.openr66.context.R66FiniteDualStates.ENDREQUESTR;
import static com.t3c.anchel.openr66.context.R66FiniteDualStates.ENDREQUESTS;
import static com.t3c.anchel.openr66.context.R66FiniteDualStates.ENDTRANSFERR;
import static com.t3c.anchel.openr66.context.R66FiniteDualStates.ENDTRANSFERS;
import static com.t3c.anchel.openr66.context.R66FiniteDualStates.ERROR;
import static com.t3c.anchel.openr66.context.R66FiniteDualStates.REQUESTD;
import static com.t3c.anchel.openr66.context.R66FiniteDualStates.REQUESTR;
import static com.t3c.anchel.openr66.context.R66FiniteDualStates.VALID;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.file.DataBlock;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import com.t3c.anchel.common.DbConfiguration;
import com.t3c.anchel.openr66.context.ErrorCode;
import com.t3c.anchel.openr66.context.R66Result;
import com.t3c.anchel.openr66.context.task.AbstractTask;
import com.t3c.anchel.openr66.context.task.TaskType;
import com.t3c.anchel.openr66.context.task.exception.OpenR66RunnerErrorException;
import com.t3c.anchel.openr66.database.DbConstant;
import com.t3c.anchel.openr66.database.data.DbRule;
import com.t3c.anchel.openr66.database.data.DbTaskRunner;
import com.t3c.anchel.openr66.protocol.configuration.Configuration;
import com.t3c.anchel.openr66.protocol.configuration.Messages;
import com.t3c.anchel.openr66.protocol.configuration.PartnerConfiguration;
import com.t3c.anchel.openr66.protocol.exception.OpenR66DatabaseGlobalException;
import com.t3c.anchel.openr66.protocol.exception.OpenR66Exception;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolBusinessQueryAlreadyFinishedException;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolBusinessQueryStillRunningException;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolNoDataException;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolNotYetConnectionException;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolPacketException;
import com.t3c.anchel.openr66.protocol.exception.OpenR66ProtocolSystemException;
import com.t3c.anchel.openr66.protocol.localhandler.packet.DataPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.EndRequestPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.EndTransferPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.ErrorPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.JsonCommandPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.LocalPacketFactory;
import com.t3c.anchel.openr66.protocol.localhandler.packet.RequestPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.ValidPacket;
import com.t3c.anchel.openr66.protocol.localhandler.packet.json.RequestJsonPacket;
import com.t3c.anchel.openr66.protocol.networkhandler.NetworkTransaction;
import com.t3c.anchel.openr66.protocol.utils.ChannelCloseTimer;
import com.t3c.anchel.openr66.protocol.utils.ChannelUtils;
import com.t3c.anchel.openr66.protocol.utils.FileUtils;
import com.t3c.anchel.openr66.protocol.utils.R66Future;
import com.t3c.anchel.storageregistration.Implements.AccessClass;
import com.t3c.anchel.storageregistration.Implements.StorageAwsImpl;

import io.netty.channel.Channel;
import io.netty.channel.local.LocalChannel;

/**
 * Class to implement actions related to real transfer: request initialization,
 * data transfer, end of transfer and of request, changing filename or filesize.
 * 
 * @author "Frederic Bregier"
 *
 */
public class TransferActions extends ServerActions {
	/**
	 * Internal Logger
	 */
	private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(TransferActions.class);

	public TransferActions() {
	}

	/**
	 * Finalize a request initialization in error
	 * 
	 * @param channel
	 * @param code
	 * @param runner
	 * @param e1
	 * @param packet
	 * @throws OpenR66ProtocolPacketException
	 */
	private final void endInitRequestInError(Channel channel, ErrorCode code, DbTaskRunner runner, OpenR66Exception e1,
			RequestPacket packet) throws OpenR66ProtocolPacketException {
		logger.error("TaskRunner initialisation in error: " + code.mesg + " " + session + " {} runner {}",
				e1 != null ? e1.getMessage() : "no exception", (runner != null ? runner.toShortString() : "no runner"));
		logger.debug("DEBUG Full stack", e1);
		localChannelReference.invalidateRequest(new R66Result(e1, session, true, code, null));

		if (packet.isToValidate()) {
			// / answer with a wrong request since runner is not set on remote
			// host
			if (runner != null) {
				if (runner.isSender()) {
					// In case Wildcard was used
					logger.debug("New FILENAME: {}", runner.getOriginalFilename());
					packet.setFilename(runner.getOriginalFilename());
					logger.debug("Rank set: " + runner.getRank());
					packet.setRank(runner.getRank());
				} else {
					logger.debug("Rank set: " + runner.getRank());
					packet.setRank(runner.getRank());
				}
			}
			packet.validate();
			packet.setCode(code.code);
			session.newState(ERROR);
			ChannelUtils.writeAbstractLocalPacket(localChannelReference, packet, true);
		} else {
			session.newState(ERROR);
			ErrorPacket error = new ErrorPacket("TaskRunner initialisation in error: " + e1.getMessage() + " for "
					+ packet.toString() + " since " + code.mesg, code.getCode(), ErrorPacket.FORWARDCLOSECODE);
			ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
		}
		session.setStatus(47);
		ChannelCloseTimer.closeFutureChannel(channel);
	}

	/**
	 * Receive a request of Transfer
	 * 
	 * @param channel
	 * @param packet
	 * @throws OpenR66ProtocolNoDataException
	 * @throws OpenR66ProtocolPacketException
	 * @throws OpenR66ProtocolBusinessException
	 * @throws OpenR66ProtocolSystemException
	 * @throws OpenR66RunnerErrorException
	 */
	public void request(LocalChannel channel, RequestPacket packet)
			throws OpenR66ProtocolNoDataException, OpenR66ProtocolPacketException, OpenR66RunnerErrorException,
			OpenR66ProtocolSystemException, OpenR66ProtocolBusinessException {
		session.setStatus(99);
		if (!session.isAuthenticated()) {
			session.setStatus(48);
			throw new OpenR66ProtocolNotAuthenticatedException(Messages.getString("LocalServerHandler.3")); //$NON-NLS-1$
		}
		if (packet.isToValidate()) {
			session.newState(REQUESTR);
		}
		// XXX validLimit only on requested side
		if (packet.isToValidate()) {
			if (Configuration.configuration.isShutdown()) {
				logger.warn(Messages.getString("LocalServerHandler.7") //$NON-NLS-1$
						+ packet.getRulename() + " from " + session.getAuth().toString());
				session.setStatus(100);
				endInitRequestInError(channel, ErrorCode.ServerOverloaded, null,
						new OpenR66ProtocolNotYetConnectionException("All new Request blocked"), packet);
				session.setStatus(100);
				return;
			}
			if (Configuration.configuration.getConstraintLimitHandler().checkConstraints()) {
				if (Configuration.configuration.getR66Mib() != null) {
					Configuration.configuration.getR66Mib().notifyOverloaded(
							"Rule: " + packet.getRulename() + " from " + session.getAuth().toString(),
							Configuration.configuration.getConstraintLimitHandler().lastAlert);
				}
				logger.warn(Messages.getString("LocalServerHandler.8") //$NON-NLS-1$
						+ packet.getRulename() + " while "
						+ Configuration.configuration.getConstraintLimitHandler().lastAlert + " from "
						+ session.getAuth().toString());
				session.setStatus(100);
				endInitRequestInError(channel, ErrorCode.ServerOverloaded, null,
						new OpenR66ProtocolNotYetConnectionException(
								"Limit exceeded " + Configuration.configuration.getConstraintLimitHandler().lastAlert),
						packet);
				session.setStatus(100);
				return;
			}
		} else if (packet.getCode() == ErrorCode.ServerOverloaded.code) {
			// XXX unvalid limit on requested host received
			logger.info("TaskRunner initialisation in error: " + ErrorCode.ServerOverloaded.mesg);
			localChannelReference
					.invalidateRequest(new R66Result(null, session, true, ErrorCode.ServerOverloaded, null));
			session.setStatus(101);
			ChannelCloseTimer.closeFutureChannel(channel);
			return;
		}
		DbRule rule;
		try {
			rule = new DbRule(localChannelReference.getDbSession(), packet.getRulename());
		} catch (WaarpDatabaseException e) {
			logger.info("Rule is unknown: " + packet.getRulename() + " {}", e.getMessage());
			session.setStatus(49);
			endInitRequestInError(channel, ErrorCode.QueryRemotelyUnknown, null,
					new OpenR66ProtocolBusinessException(Messages.getString("LocalServerHandler.9") + //$NON-NLS-1$
							packet.getRulename()),
					packet);
			return;
		}
		int blocksize = packet.getBlocksize();
		if (packet.isToValidate()) {
			if (!rule.checkHostAllow(session.getAuth().getUser())) {
				session.setStatus(30);
				throw new OpenR66ProtocolNotAuthenticatedException(Messages.getString("LocalServerHandler.10")); //$NON-NLS-1$
			}
			// Check if the blocksize is greater than local value
			if (Configuration.configuration.getBLOCKSIZE() < blocksize) {
				blocksize = Configuration.configuration.getBLOCKSIZE();
				String sep = localChannelReference.getPartner().getSeperator();
				packet = new RequestPacket(packet.getRulename(), packet.getMode(), packet.getFilename(), blocksize,
						packet.getRank(), packet.getSpecialId(), packet.getFileInformation(), packet.getOriginalSize(),
						sep);
			}
		}
		if (!RequestPacket.isCompatibleMode(rule.getMode(), packet.getMode())) {
			// not compatible Rule and mode in request
			throw new OpenR66ProtocolNotAuthenticatedException(
					Messages.getString("LocalServerHandler.12") + rule.getMode() + " vs " //$NON-NLS-1$
							+ packet.getMode());
		}
		session.setBlockSize(blocksize);
		DbTaskRunner runner;
		// requested
		boolean isRetrieve = DbTaskRunner.getSenderByRequestPacket(packet);
		if (packet.getSpecialId() != DbConstant.ILLEGALVALUE) {
			// Reload or create
			String requested = DbTaskRunner.getRequested(session, packet);
			String requester = DbTaskRunner.getRequester(session, packet);
			logger.debug("DEBUG: " + packet.getSpecialId() + ":" + isRetrieve);
			if (packet.isToValidate()) {
				// Id could be a creation or a reload
				// Try reload
				try {
					runner = new DbTaskRunner(localChannelReference.getDbSession(), session, rule,
							packet.getSpecialId(), requester, requested);
					// Patch to prevent self request to be stored by sender

					// TODO Integration to S3 (File download)

					if (runner.getMode() == 2) {
						String outdirpath = session.getDir().getFullPath() + "out" + File.separator;
						String filename = new File(runner.getFilename()).getName();
						String outDirFile = outdirpath.concat(filename);
						new StorageAwsImpl().GetById(outDirFile);
					}

					boolean ignoreSave = runner.shallIgnoreSave();
					runner.setSender(isRetrieve);
					logger.debug("DEBUG: " + runner.getSpecialId() + ":" + ignoreSave + ":" + runner.shallIgnoreSave()
							+ ":" + isRetrieve);
					if (ignoreSave && !runner.shallIgnoreSave() && !runner.checkFromDbForSubmit()) {
						// Since status changed, it means that object should be
						// created and not reloaded
						// But in case of submit, item already exist so shall be
						// loaded from database
						throw new WaarpDatabaseNoDataException("False load, must reopen and create DbTaskRunner");
					}
				} catch (WaarpDatabaseNoDataException e) {
					// Reception of request from requester host
					try {
						runner = new DbTaskRunner(localChannelReference.getDbSession(), session, rule, isRetrieve,
								packet);
						logger.debug("Runner before any action: {} {}", runner.shallIgnoreSave(), runner);
					} catch (WaarpDatabaseException e1) {
						session.setStatus(33);
						endInitRequestInError(channel, ErrorCode.QueryRemotelyUnknown, null,
								new OpenR66DatabaseGlobalException(e), packet);
						return;
					}
				} catch (WaarpDatabaseException e) {
					session.setStatus(34);
					endInitRequestInError(channel, ErrorCode.QueryRemotelyUnknown, null,
							new OpenR66DatabaseGlobalException(e), packet);
					return;
				}
				if (runner.isAllDone()) {
					// truly an error since done
					session.setStatus(31);
					endInitRequestInError(channel, ErrorCode.QueryAlreadyFinished, runner,
							new OpenR66ProtocolBusinessQueryAlreadyFinishedException(
									Messages.getString("LocalServerHandler.13") //$NON-NLS-1$
											+ packet.getSpecialId()),
							packet);
					return;
				}
				LocalChannelReference lcr = Configuration.configuration.getLocalTransaction()
						.getFromRequest(requested + " " + requester + " " + packet.getSpecialId());
				if (lcr != null) {
					// truly an error since still running
					session.setStatus(32);
					endInitRequestInError(channel, ErrorCode.QueryStillRunning, runner,
							new OpenR66ProtocolBusinessQueryStillRunningException(
									Messages.getString("LocalServerHandler.14") //$NON-NLS-1$
											+ packet.getSpecialId()),
							packet);
					return;
				}
				logger.debug("Runner before any action: {} {}", runner.shallIgnoreSave(), runner);
				// ok to restart
				try {
					if (runner.restart(false)) {
						runner.saveStatus();
					}
				} catch (OpenR66RunnerErrorException e) {
				}
				// Change the SpecialID! => could generate an error ?
				packet.setSpecialId(runner.getSpecialId());
			} else {
				// Id should be a reload
				try {
					runner = new DbTaskRunner(localChannelReference.getDbSession(), session, rule,
							packet.getSpecialId(), requester, requested);
				} catch (WaarpDatabaseException e) {
					if (localChannelReference.getDbSession() == null) {
						// Special case of no database client
						try {
							runner = new DbTaskRunner(localChannelReference.getDbSession(), session, rule, isRetrieve,
									packet);
							logger.debug("Runner before any action: {} {}", runner.shallIgnoreSave(), runner);
						} catch (WaarpDatabaseException e1) {
							session.setStatus(35);
							endInitRequestInError(channel, ErrorCode.QueryRemotelyUnknown, null,
									new OpenR66DatabaseGlobalException(e1), packet);
							return;
						}
					} else {
						endInitRequestInError(channel, ErrorCode.QueryRemotelyUnknown, null,
								new OpenR66DatabaseGlobalException(e), packet);
						session.setStatus(36);
						return;
					}
				}
				runner.setSender(isRetrieve);
				// FIX check for SelfRequest
				if (runner.isSelfRequest()) {
					runner.setFilename(runner.getOriginalFilename());
				}
				if (!runner.isSender()) {
					logger.debug("New filename ? :" + packet.getFilename());
					runner.setOriginalFilename(packet.getFilename());
					if (runner.getRank() == 0) {
						runner.setFilename(packet.getFilename());
					}
				}
				logger.debug("Runner before any action: {} {}", runner.shallIgnoreSave(), runner);
				try {
					if (runner.restart(false)) {
						if (!runner.isSelfRequest()) {
							runner.saveStatus();
						}
					}
				} catch (OpenR66RunnerErrorException e) {
				}
			}
		} else {
			// Very new request
			// should not be the case (the requester should always set the id)
			logger.error("NO TransferID specified: SHOULD NOT BE THE CASE");
			try {
				runner = new DbTaskRunner(localChannelReference.getDbSession(), session, rule, isRetrieve, packet);
			} catch (WaarpDatabaseException e) {
				session.setStatus(37);
				endInitRequestInError(channel, ErrorCode.QueryRemotelyUnknown, null,
						new OpenR66DatabaseGlobalException(e), packet);
				return;
			}
			packet.setSpecialId(runner.getSpecialId());
		}
		logger.debug("Runner before any action: {} {}", runner.shallIgnoreSave(), runner);
		// Check now if request is a valid one
		if (packet.getCode() != ErrorCode.InitOk.code) {
			// not valid so create an error from there
			ErrorCode code = ErrorCode.getFromCode("" + packet.getCode());
			session.setBadRunner(runner, code);
			if (!runner.shallIgnoreSave()) {
				runner.saveStatus();
			}
			session.newState(ERROR);
			logger.error("Bad runner at startup {} {}", packet, session);
			ErrorPacket errorPacket = new ErrorPacket(code.mesg, code.getCode(), ErrorPacket.FORWARDCLOSECODE);
			errorMesg(channel, errorPacket);
			return;
		}
		// Receiver can specify a rank different from database
		if (runner.isSender()) {
			logger.debug("Rank was: " + runner.getRank() + " -> " + packet.getRank());
			runner.setRankAtStartup(packet.getRank());
		} else {
			if (runner.getRank() > packet.getRank()) {
				logger.debug("Recv Rank was: " + runner.getRank() + " -> " + packet.getRank());
				// if receiver, change only if current rank is upper proposed
				// rank
				runner.setRankAtStartup(packet.getRank());
			}
			if (packet.getOriginalSize() > 0) {
				runner.setOriginalSize(packet.getOriginalSize());
			}
		}
		logger.debug("Filesize: " + packet.getOriginalSize() + ":" + runner.isSender());
		boolean shouldInformBack = false;
		try {
			session.setRunner(runner);
			// Fix to ensure that recv request are not trying to access to not
			// chroot files
			if (Configuration.configuration.isChrootChecked() && packet.isToValidate() && runner.isSender()) {
				session.startup(true);
			} else {
				session.startup(false);
			}
			if (runner.isSender() && !runner.isSendThrough()) {
				if (packet.getOriginalSize() != runner.getOriginalSize()) {
					packet.setOriginalSize(runner.getOriginalSize());
					shouldInformBack = true;
					logger.debug("Filesize2: " + packet.getOriginalSize() + ":" + runner.isSender());
				}
			}
		} catch (OpenR66RunnerErrorException e) {
			try {
				runner.saveStatus();
			} catch (OpenR66RunnerErrorException e1) {
				logger.error("Cannot save Status: " + runner, e1);
			}
			if (runner.getErrorInfo() == ErrorCode.InitOk || runner.getErrorInfo() == ErrorCode.PreProcessingOk
					|| runner.getErrorInfo() == ErrorCode.TransferOk) {
				runner.setErrorExecutionStatus(ErrorCode.ExternalOp);
			}
			logger.error("PreTask in error {}", e.getMessage(), e);
			errorToSend("PreTask in error: " + e.getMessage(), runner.getErrorInfo(), channel, 38);
			return;
		}
		logger.debug("Filesize: " + packet.getOriginalSize() + ":" + runner.isSender());
		if (!shouldInformBack) {
			shouldInformBack = !packet.getFileInformation().equals(runner.getFileInformation());
		}
		if (runner.isFileMoved() && runner.isSender() && runner.isInTransfer() && runner.getRank() == 0
				&& (!packet.isToValidate())) {
			// File was moved during PreTask and very beginning of the transfer
			// and the remote host has already received the request packet
			// => Informs the receiver of the new name
			sendFilenameFilesizeChanging(packet, runner, "Will send a modification of filename due to pretask: ",
					"Change Filename by Pre action on sender");
		} else if ((!packet.getFilename().equals(runner.getOriginalFilename())) && runner.isSender()
				&& runner.isInTransfer() && runner.getRank() == 0 && (!packet.isToValidate())) {
			// File was modify at the very beginning (using wildcards)
			// and the remote host has already received the request packet
			// => Informs the receiver of the new name
			sendFilenameFilesizeChanging(packet, runner, "Will send a modification of filename due to wildcard: ",
					"Change Filename by Wildcard on sender");
		} else if (runner.isSelfRequest() && runner.isSender() && runner.isInTransfer() && runner.getRank() == 0
				&& (!packet.isToValidate())) {
			// FIX SelfRequest
			// File could be modified at the very beginning (using wildcards)
			// and the remote host has already received the request packet
			// => Informs the receiver of the new name
			sendFilenameFilesizeChanging(packet, runner,
					"Will send a modification of filename due to wildcard in SelfMode: ",
					"Change Filename by Wildcard on sender in SelfMode");
		} else if (shouldInformBack && (!packet.isToValidate())) {
			// Was only for (shouldInformBack)
			// File length is now known, so inform back
			sendFilenameFilesizeChanging(packet, runner, "Will send a modification of filesize or fileInfo: ",
					"Change Filesize / FileInfo on sender");
		}
		session.setReady(true);
		Configuration.configuration.getLocalTransaction().setFromId(runner, localChannelReference);
		// inform back
		if (packet.isToValidate()) {
			if (Configuration.configuration.getMonitoring() != null) {
				Configuration.configuration.getMonitoring().lastInActiveTransfer = System.currentTimeMillis();
			}
			if (runner.isSender()) {
				// In case Wildcard was used
				logger.debug("New FILENAME: {}", runner.getOriginalFilename());
				packet.setFilename(runner.getOriginalFilename());
				logger.debug("Rank set: " + runner.getRank());
				packet.setRank(runner.getRank());
			} else {
				logger.debug("Rank set: " + runner.getRank());
				packet.setRank(runner.getRank());
			}
			packet.validate();
			session.newState(REQUESTD);
			ChannelUtils.writeAbstractLocalPacket(localChannelReference, packet, true);
		} else {
			session.newState(REQUESTD);
			// requester => might be a client
			// Save the runner into the session and validate the request so
			// begin transfer
			session.getLocalChannelReference().getFutureRequest().setRunner(runner);
			localChannelReference.getFutureValidRequest().setSuccess();
			if (Configuration.configuration.getMonitoring() != null) {
				Configuration.configuration.getMonitoring().lastOutActiveTransfer = System.currentTimeMillis();
			}
		}
		// if retrieve => START the retrieve operation except if in Send Through
		// mode
		if (runner.isSender()) {
			if (runner.isSendThrough()) {
				// it is legal to send data from now
				logger.debug("Now ready to continue with send through");
				localChannelReference
						.validateEndTransfer(new R66Result(session, false, ErrorCode.PreProcessingOk, runner));
			} else {
				// Automatically send data now
				logger.debug("Now ready to continue with runRetrieve");
				NetworkTransaction.runRetrieve(session, channel);
			}
		}
		session.setStatus(39);
	}

	/**
	 * Send a Filename/Filesize change to the partner
	 * 
	 * @param packet
	 * @param runner
	 * @throws OpenR66ProtocolPacketException
	 */
	private final void sendFilenameFilesizeChanging(RequestPacket packet, DbTaskRunner runner, String debug,
			String info) throws OpenR66ProtocolPacketException {
		logger.debug(debug + runner.getFilename());
		session.newState(VALID);
		if (localChannelReference.getPartner().useJson()) {
			RequestJsonPacket request = new RequestJsonPacket();
			request.setComment(info);
			request.setFilename(runner.getFilename());
			request.setFilesize(packet.getOriginalSize());
			String infoTransfer = runner.getFileInformation();
			if (infoTransfer != null && !infoTransfer.equals(packet.getFileInformation())) {
				request.setFileInfo(runner.getFileInformation());
			}
			JsonCommandPacket validPacket = new JsonCommandPacket(request, LocalPacketFactory.REQUESTPACKET);
			ChannelUtils.writeAbstractLocalPacket(localChannelReference, validPacket, true);
		} else {
			String infoTransfer = runner.getFileInformation();
			ValidPacket validPacket;
			if (infoTransfer != null && !infoTransfer.equals(packet.getFileInformation())
					&& localChannelReference.getPartner().changeFileInfoEnabled()) {
				validPacket = new ValidPacket(info,
						runner.getFilename() + PartnerConfiguration.BAR_SEPARATOR_FIELD + packet.getOriginalSize()
								+ PartnerConfiguration.BAR_SEPARATOR_FIELD + packet.getFileInformation(),
						LocalPacketFactory.REQUESTPACKET);
			} else {
				validPacket = new ValidPacket(info,
						runner.getFilename() + PartnerConfiguration.BAR_SEPARATOR_FIELD + packet.getOriginalSize(),
						LocalPacketFactory.REQUESTPACKET);
			}
			ChannelUtils.writeAbstractLocalPacket(localChannelReference, validPacket, true);
		}
	}

	/**
	 * Send an error
	 * 
	 * @param message
	 * @param code
	 * @param channel
	 * @throws OpenR66ProtocolPacketException
	 */
	private final void errorToSend(String message, ErrorCode code, Channel channel, int status)
			throws OpenR66ProtocolPacketException {
		session.newState(ERROR);
		try {
			session.setFinalizeTransfer(false, new R66Result(new OpenR66ProtocolPacketException(message), session, true,
					code, session.getRunner()));
		} catch (OpenR66RunnerErrorException e1) {
			localChannelReference.invalidateRequest(new R66Result(e1, session, true, code, session.getRunner()));
		} catch (OpenR66ProtocolSystemException e1) {
			localChannelReference.invalidateRequest(new R66Result(e1, session, true, code, session.getRunner()));
		}
		ErrorPacket error = new ErrorPacket(message, code.getCode(), ErrorPacket.FORWARDCLOSECODE);
		ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
		session.setStatus(status);
		ChannelCloseTimer.closeFutureChannel(channel);
	}

	/**
	 * Receive a data block
	 * 
	 * @param channel
	 * @param packet
	 * @throws OpenR66ProtocolNotAuthenticatedException
	 * @throws OpenR66ProtocolBusinessException
	 * @throws OpenR66ProtocolPacketException
	 */
	public void data(Channel channel, DataPacket packet) throws OpenR66ProtocolNotAuthenticatedException,
			OpenR66ProtocolBusinessException, OpenR66ProtocolPacketException {
		if (!session.isAuthenticated()) {
			logger.debug("Not authenticated while Data received");
			packet.clear();
			throw new OpenR66ProtocolNotAuthenticatedException("Not authenticated while Data received");
		}
		if (!session.isReady()) {
			logger.debug("No request prepared");
			packet.clear();
			throw new OpenR66ProtocolBusinessException("No request prepared");
		}
		if (session.getRunner().isSender()) {
			logger.debug("Not in receive MODE but receive a packet");
			packet.clear();
			throw new OpenR66ProtocolBusinessException("Not in receive MODE but receive a packet");
		}
		if (!session.getRunner().continueTransfer()) {
			logger.debug("EndTransfer failed ? " + localChannelReference.getFutureEndTransfer().isFailed());
			if (localChannelReference.getFutureEndTransfer().isFailed()) {
				// nothing to do since already done
				session.setStatus(94);
				packet.clear();
				return;
			}
			errorToSend("Transfer in error due previously aborted transmission", ErrorCode.TransferError, channel, 95);
			packet.clear();
			return;
		}
		if (packet.getPacketRank() != session.getRunner().getRank()) {
			logger.debug("Issue on rank: " + packet.getPacketRank() + ":" + session.getRunner().getRank());
			if (!session.addError()) {
				// cannot continue
				logger.error(Messages.getString("LocalServerHandler.15") + packet.getPacketRank() + " : " + //$NON-NLS-1$
						session.getRunner().getRank() + " from {}", session.getRunner());
				errorToSend("Too much Bad Rank in transmission: " + packet.getPacketRank(), ErrorCode.TransferError,
						channel, 96);
				packet.clear();
				return;
			}
			// Fix the rank if possible
			if (packet.getPacketRank() < session.getRunner().getRank()) {
				logger.debug("Bad RANK: " + packet.getPacketRank() + " : " + session.getRunner().getRank());
				session.getRunner().setRankAtStartup(packet.getPacketRank());
				session.getRestart().restartMarker(session.getRunner().getBlocksize() * session.getRunner().getRank());
				try {
					session.getFile().restartMarker(session.getRestart());
				} catch (CommandAbstractException e) {
					logger.error("Bad RANK: " + packet.getPacketRank() + " : " + session.getRunner().getRank());
					errorToSend("Bad Rank in transmission even after retry: " + packet.getPacketRank(),
							ErrorCode.TransferError, channel, 96);
					packet.clear();
					return;
				}
			} else {
				// really bad
				logger.error("Bad RANK: " + packet.getPacketRank() + " : " + session.getRunner().getRank());
				errorToSend(
						"Bad Rank in transmission: " + packet.getPacketRank() + " > " + session.getRunner().getRank(),
						ErrorCode.TransferError, channel, 20);
				packet.clear();
				return;
			}
		}
		// Check global size
		long originalSize = session.getRunner().getOriginalSize();
		if (originalSize >= 0) {
			if (session.getRunner().getBlocksize() * (session.getRunner().getRank() - 1) > originalSize) {
				// cannot continue
				logger.error(
						Messages.getString("LocalServerHandler.16") + packet.getPacketRank() + " : " + //$NON-NLS-1$
								(originalSize / session.getRunner().getBlocksize() + 1) + " from {}",
						session.getRunner());
				errorToSend("Too much data transferred: " + packet.getPacketRank(), ErrorCode.TransferError, channel,
						96);
				packet.clear();
				return;
			}
		}
		// if MD5 check MD5
		if (RequestPacket.isMD5Mode(session.getRunner().getMode())) {
			logger.debug("AlgoDigest: " + (localChannelReference.getPartner() != null
					? localChannelReference.getPartner().getDigestAlgo() : "usual algo"));
			if (!packet.isKeyValid(localChannelReference.getPartner().getDigestAlgo())) {
				// Wrong packet
				logger.error(Messages.getString("LocalServerHandler.17"), packet, //$NON-NLS-1$
						localChannelReference.getPartner().getDigestAlgo().name);
				errorToSend(
						"Transfer in error due to bad Hash on data packet ("
								+ localChannelReference.getPartner().getDigestAlgo().name + ")",
						ErrorCode.MD5Error, channel, 21);
				packet.clear();
				return;
			}
		}
		if (Configuration.configuration.isGlobalDigest()) {
			if (globalDigest == null) {
				try {
					// check if first block, since if not, digest will be only
					// partial
					if (session.getRunner().getRank() > 0) {
						localChannelReference.setPartialHash();
					}
					if (localChannelReference.getPartner() != null) {
						if (localChannelReference.getPartner().useFinalHash()) {
							DigestAlgo algo = localChannelReference.getPartner().getDigestAlgo();
							if (algo != Configuration.configuration.getDigest()) {
								globalDigest = new FilesystemBasedDigest(algo);
								localDigest = new FilesystemBasedDigest(Configuration.configuration.getDigest());
							}
						}
					}
					if (globalDigest == null) {
						globalDigest = new FilesystemBasedDigest(Configuration.configuration.getDigest());
						localDigest = null;
					}
				} catch (NoSuchAlgorithmException e) {
				}
				logger.debug("GlobalDigest: " + localChannelReference.getPartner().getDigestAlgo() + " different? "
						+ (localDigest != null));
			}
			FileUtils.computeGlobalHash(globalDigest, packet.getData());
			if (localDigest != null) {
				FileUtils.computeGlobalHash(localDigest, packet.getData());
			}
		}
		DataBlock dataBlock = new DataBlock();
		if (session.getRunner().isRecvThrough() && localChannelReference.isRecvThroughMode()) {
			try {
				localChannelReference.getRecvThroughHandler().writeByteBuf(packet.getData());
				session.getRunner().incrementRank();
				if (packet.getPacketRank() % 100 == 1) {
					logger.debug("Good RANK: " + packet.getPacketRank() + " : " + session.getRunner().getRank());
				}
			} finally {
				packet.clear();
			}
		} else {
			dataBlock.setBlock(packet.getData());
			try {
				session.getFile().writeDataBlock(dataBlock);
				session.getRunner().incrementRank();
				if (packet.getPacketRank() % 100 == 1) {
					logger.debug("Good RANK: " + packet.getPacketRank() + " : " + session.getRunner().getRank());
				}
			} catch (FileTransferException e) {
				errorToSend("Transfer in error", ErrorCode.TransferError, channel, 22);
				return;
			} finally {
				dataBlock.clear();
				packet.clear();
			}
		}
	}

	/**
	 * Receive an End of Transfer
	 * 
	 * @param channel
	 * @param packet
	 * @throws OpenR66RunnerErrorException
	 * @throws OpenR66ProtocolSystemException
	 * @throws OpenR66ProtocolNotAuthenticatedException
	 */
	public void endTransfer(Channel channel, EndTransferPacket packet) throws OpenR66RunnerErrorException,
			OpenR66ProtocolSystemException, OpenR66ProtocolNotAuthenticatedException {
		if (!session.isAuthenticated()) {
			throw new OpenR66ProtocolNotAuthenticatedException("Not authenticated while EndTransfer received");
		}
		// Check end of transfer
		long originalSize = session.getRunner().getOriginalSize();
		logger.debug("OSize: " + originalSize + " isSender: " + session.getRunner().isSender());
		if (packet.isToValidate()) {
			// check if possible originalSize
			if (originalSize > 0) {
				try {
					if (!session.getRunner().isRecvThrough() && session.getFile().length() != originalSize
							|| session.getFile().length() == 0) {
						R66Result result = new R66Result(
								new OpenR66RunnerErrorException(Messages.getString("LocalServerHandler.18")), //$NON-NLS-1$
								session, true, ErrorCode.TransferError, session.getRunner());
						try {
							session.setFinalizeTransfer(false, result);
						} catch (OpenR66RunnerErrorException e) {
						} catch (OpenR66ProtocolSystemException e) {
						}
						ErrorPacket error = new ErrorPacket(
								"Final size in error, transfer in error and rank should be reset to 0",
								ErrorCode.TransferError.getCode(), ErrorPacket.FORWARDCLOSECODE);
						try {
							ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
						} catch (OpenR66ProtocolPacketException e) {
						}
						session.setStatus(23);
						ChannelCloseTimer.closeFutureChannel(channel);
						return;
					}
				} catch (CommandAbstractException e) {
					// ignore
				}
			}
			// check if possible Global Digest
			String hash = packet.getOptional();
			logger.debug("GlobalDigest: " + localChannelReference.getPartner().getDigestAlgo() + " different? "
					+ (localDigest != null) + " remoteHash? " + (hash != null));
			if (hash != null && globalDigest != null) {
				String localhash = FilesystemBasedDigest.getHex(globalDigest.Final());
				globalDigest = null;
				if (!localhash.equalsIgnoreCase(hash)) {
					// bad global Hash
					// session.getRunner().setRankAtStartup(0);
					R66Result result = new R66Result(
							new OpenR66RunnerErrorException(Messages.getString("LocalServerHandler.19") + //$NON-NLS-1$
									localChannelReference.getPartner().getDigestAlgo().name + ")"),
							session, true, ErrorCode.MD5Error, session.getRunner());
					try {
						session.setFinalizeTransfer(false, result);
					} catch (OpenR66RunnerErrorException e) {
					} catch (OpenR66ProtocolSystemException e) {
					}
					ErrorPacket error = new ErrorPacket(
							"Global Hash in error, transfer in error and rank should be reset to 0 (using "
									+ localChannelReference.getPartner().getDigestAlgo().name + ")",
							ErrorCode.MD5Error.getCode(), ErrorPacket.FORWARDCLOSECODE);
					try {
						ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
					} catch (OpenR66ProtocolPacketException e) {
					}
					session.setStatus(23);
					ChannelCloseTimer.closeFutureChannel(channel);
					return;
				} else {
					if (localDigest != null) {
						localhash = FilesystemBasedDigest.getHex(localDigest.Final());
					}
					localChannelReference.setHashComputeDuringTransfer(localhash);
					logger.debug("Global digest ok");
				}
			} else if (globalDigest != null) {
				String localhash = null;
				if (localDigest != null) {
					localhash = FilesystemBasedDigest.getHex(localDigest.Final());
				} else {
					localhash = FilesystemBasedDigest.getHex(globalDigest.Final());
				}
				globalDigest = null;
				localChannelReference.setHashComputeDuringTransfer(localhash);
			}
			localDigest = null;
			globalDigest = null;
			session.newState(ENDTRANSFERS);
			if (!localChannelReference.getFutureRequest().isDone()) {
				// Finish with post Operation
				R66Result result = new R66Result(session, false, ErrorCode.TransferOk, session.getRunner());
				session.newState(ENDTRANSFERR);
				try {
					session.setFinalizeTransfer(true, result);
				} catch (OpenR66RunnerErrorException e) {
					// TODO
					session.newState(ERROR);
					ErrorPacket error = null;
					if (localChannelReference.getFutureRequest().getResult() != null) {
						result = localChannelReference.getFutureRequest().getResult();
						error = new ErrorPacket("Error while finalizing transfer: " + result.getMessage(),
								result.getCode().getCode(), ErrorPacket.FORWARDCLOSECODE);
					} else {
						error = new ErrorPacket("Error while finalizing transfer", ErrorCode.FinalOp.getCode(),
								ErrorPacket.FORWARDCLOSECODE);
					}
					try {
						ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
					} catch (OpenR66ProtocolPacketException e1) {
					}
					session.setStatus(23);
					ChannelCloseTimer.closeFutureChannel(channel);
					return;
				} catch (OpenR66ProtocolSystemException e) {
					// TODO
					session.newState(ERROR);
					ErrorPacket error = null;
					if (localChannelReference.getFutureRequest().getResult() != null) {
						result = localChannelReference.getFutureRequest().getResult();
						error = new ErrorPacket("Error while finalizing transfer: " + result.getMessage(),
								result.getCode().getCode(), ErrorPacket.FORWARDCLOSECODE);
					} else {
						error = new ErrorPacket("Error while finalizing transfer", ErrorCode.FinalOp.getCode(),
								ErrorPacket.FORWARDCLOSECODE);
					}
					try {
						ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
					} catch (OpenR66ProtocolPacketException e1) {
					}
					session.setStatus(23);
					ChannelCloseTimer.closeFutureChannel(channel);
					return;
				}
				// Now can send validation
				packet.validate();
				try {
					ChannelUtils.writeAbstractLocalPacket(localChannelReference, packet, false);
				} catch (OpenR66ProtocolPacketException e) {
					// ignore
				}
			} else {
				// in error due to a previous status (like bad MD5)
				logger.error(Messages.getString("LocalServerHandler.20")); //$NON-NLS-1$
				session.setStatus(23);
				channel.close();
				return;
			}
		} else {
			session.newState(ENDTRANSFERR);
			if (!localChannelReference.getFutureRequest().isDone()) {
				// Validation of end of transfer
				R66Result result = new R66Result(session, false, ErrorCode.TransferOk, session.getRunner());
				try {
					session.setFinalizeTransfer(true, result);
				} catch (OpenR66RunnerErrorException e) {
					// TODO
					session.newState(ERROR);
					ErrorPacket error = null;
					if (localChannelReference.getFutureRequest().getResult() != null) {
						result = localChannelReference.getFutureRequest().getResult();
						error = new ErrorPacket("Error while finalizing transfer: " + result.getMessage(),
								result.getCode().getCode(), ErrorPacket.FORWARDCLOSECODE);
					} else {
						error = new ErrorPacket("Error while finalizing transfer", ErrorCode.FinalOp.getCode(),
								ErrorPacket.FORWARDCLOSECODE);
					}
					try {
						ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
					} catch (OpenR66ProtocolPacketException e1) {
					}
					session.setStatus(23);
					ChannelCloseTimer.closeFutureChannel(channel);
					return;
				} catch (OpenR66ProtocolSystemException e) {
					// TODO
					session.newState(ERROR);
					ErrorPacket error = null;
					if (localChannelReference.getFutureRequest().getResult() != null) {
						result = localChannelReference.getFutureRequest().getResult();
						error = new ErrorPacket("Error while finalizing transfer: " + result.getMessage(),
								result.getCode().getCode(), ErrorPacket.FORWARDCLOSECODE);
					} else {
						error = new ErrorPacket("Error while finalizing transfer", ErrorCode.FinalOp.getCode(),
								ErrorPacket.FORWARDCLOSECODE);
					}
					try {
						ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
					} catch (OpenR66ProtocolPacketException e1) {
					}
					session.setStatus(23);
					ChannelCloseTimer.closeFutureChannel(channel);
					return;
				}
			}
		}
	}

	/**
	 * Receive an End of Request
	 * 
	 * @param channel
	 * @param packet
	 * @throws OpenR66RunnerErrorException
	 * @throws OpenR66ProtocolSystemException
	 * @throws OpenR66ProtocolNotAuthenticatedException
	 */
	public void endRequest(Channel channel, EndRequestPacket packet) {
		// Validate the last post action on a transfer from receiver remote host
		logger.info("Valid Request {} Packet {}", localChannelReference, packet);
		DbTaskRunner runner = session.getRunner();
		logger.debug("Runner endRequest: " + (session.getRunner() != null));
		if (runner != null) {
			runner.setAllDone();
			try {
				runner.saveStatus();
			} catch (OpenR66RunnerErrorException e) {
				// ignore
			}
			runner.clean();
		}
		String optional = null;
		if (session.getExtendedProtocol()) {
			optional = packet.getOptional();
		}
		if (!localChannelReference.getFutureRequest().isDone()) {
			// end of request
			R66Future transfer = localChannelReference.getFutureEndTransfer();
			try {
				transfer.await();
			} catch (InterruptedException e) {
			}
			if (transfer.isSuccess()) {
				if (session.getExtendedProtocol() && session.getBusinessObject() != null) {
					if (session.getBusinessObject().getInfo(session) == null) {
						session.getBusinessObject().setInfo(session, optional);
					} else {
						String temp = session.getBusinessObject().getInfo(session);
						session.getBusinessObject().setInfo(session, optional);
						optional = temp;
					}
				} else if (session.getExtendedProtocol() && transfer.getResult().getOther() == null
						&& optional != null) {
					transfer.getResult().setOther(optional);
				}
				localChannelReference.validateRequest(transfer.getResult());
			}
		}
		session.setStatus(1);
		if (packet.isToValidate()) {
			session.newState(ENDREQUESTS);
			packet.validate();
			if (session.getExtendedProtocol()) {
				packet.setOptional(optional);
			}
			session.newState(ENDREQUESTR);
			try {
				ChannelUtils.writeAbstractLocalPacket(localChannelReference, packet, true);
			} catch (OpenR66ProtocolPacketException e) {
			}
		} else {
			session.newState(ENDREQUESTR);
		}
		if (runner != null && (runner.isSelfRequested() || runner.isSelfRequest())) {
			ChannelCloseTimer.closeFutureChannel(channel);
		}
	}

	/**
	 * If newFileInfo is provided and different than current value
	 * 
	 * @param channel
	 * @param newFileInfo
	 * @throws OpenR66RunnerErrorException
	 */
	public void requestChangeFileInfo(Channel channel, String newFileInfo) throws OpenR66RunnerErrorException {
		DbTaskRunner runner = session.getRunner();
		logger.debug("NewFileInfo " + newFileInfo);
		runner.setFileInformation(newFileInfo);
		try {
			runner.update();
		} catch (WaarpDatabaseException e) {
			runner.saveStatus();
			runner.setErrorExecutionStatus(ErrorCode.ExternalOp);
			session.newState(ERROR);
			logger.error("File info changing in error {}", e.getMessage());
			ErrorPacket error = new ErrorPacket("File changing information in error: " + e.getMessage(),
					runner.getErrorInfo().getCode(), ErrorPacket.FORWARDCLOSECODE);
			try {
				ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
			} catch (OpenR66ProtocolPacketException e2) {
			}
			try {
				session.setFinalizeTransfer(false, new R66Result(new OpenR66RunnerErrorException(e), session, true,
						runner.getErrorInfo(), runner));
			} catch (OpenR66RunnerErrorException e1) {
				localChannelReference.invalidateRequest(new R66Result(new OpenR66RunnerErrorException(e), session, true,
						runner.getErrorInfo(), runner));
			} catch (OpenR66ProtocolSystemException e1) {
				localChannelReference.invalidateRequest(new R66Result(new OpenR66RunnerErrorException(e), session, true,
						runner.getErrorInfo(), runner));
			}
			session.setStatus(97);
			ChannelCloseTimer.closeFutureChannel(channel);
			return;
		}
	}

	/**
	 * Change the filename and the filesize
	 * 
	 * @param channel
	 * @param newfilename
	 * @param newSize
	 * @throws OpenR66RunnerErrorException
	 */
	public void requestChangeNameSize(Channel channel, String newfilename, long newSize)
			throws OpenR66RunnerErrorException {
		session.newState(VALID);
		DbTaskRunner runner = session.getRunner();
		logger.debug("NewSize " + newSize + " NewName " + newfilename);
		// The filename or filesize from sender is changed due to PreTask so
		// change it too in receiver
		// comment, filename, filesize
		// Close only if an error occurs!
		if (runner != null) {
			if (newSize > 0) {
				runner.setOriginalSize(newSize);
				// Check if a CHKFILE task was supposely needed to run
				String[][] rpretasks = runner.getRule().getRpreTasksArray();
				if (rpretasks != null) {
					for (String[] strings : rpretasks) {
						AbstractTask task = runner.getTask(strings, session);
						if (task.getType() == TaskType.CHKFILE) {
							// re run this in case
							task.run();
							try {
								task.getFutureCompletion().await();
							} catch (InterruptedException e) {
							}
							if (!task.getFutureCompletion().isSuccess()) {
								// not valid so create an error from there
								ErrorCode code = ErrorCode.SizeNotAllowed;
								runner.setErrorExecutionStatus(code);
								runner.saveStatus();
								session.setBadRunner(runner, code);
								session.newState(ERROR);
								logger.error("File length is not compatible with Rule or capacity {} {}",
										newfilename + " : " + newSize, session);
								ErrorPacket errorPacket = new ErrorPacket(
										"File length is not compatible with Rule or capacity", code.getCode(),
										ErrorPacket.FORWARDCLOSECODE);
								try {
									ChannelUtils.writeAbstractLocalPacket(localChannelReference, errorPacket, true);
								} catch (OpenR66ProtocolPacketException e2) {
								}
								try {
									session.setFinalizeTransfer(false,
											new R66Result(new OpenR66RunnerErrorException(errorPacket.getSheader()),
													session, true, runner.getErrorInfo(), runner));
								} catch (OpenR66RunnerErrorException e1) {
									localChannelReference.invalidateRequest(
											new R66Result(new OpenR66RunnerErrorException(errorPacket.getSheader()),
													session, true, runner.getErrorInfo(), runner));
								} catch (OpenR66ProtocolSystemException e1) {
									localChannelReference.invalidateRequest(
											new R66Result(new OpenR66RunnerErrorException(errorPacket.getSheader()),
													session, true, runner.getErrorInfo(), runner));
								}
								session.setStatus(97);
								ChannelCloseTimer.closeFutureChannel(channel);
								return;
							}
						}
					}
				}
			}
		}
		// check if send is already on going
		if (runner != null && runner.getRank() > 0) {
			// already started so not changing the filename
			// Success: No write back at all
			return;
		}
		// Pre execution was already done since this packet is only received
		// once
		// the request is already validated by the receiver
		try {
			session.renameReceiverFile(newfilename);
		} catch (OpenR66RunnerErrorException e) {
			runner.saveStatus();
			runner.setErrorExecutionStatus(ErrorCode.FileNotFound);
			session.newState(ERROR);
			logger.error("File renaming in error {}", e.getMessage());
			ErrorPacket error = new ErrorPacket("File renaming in error: " + e.getMessage(),
					runner.getErrorInfo().getCode(), ErrorPacket.FORWARDCLOSECODE);
			try {
				ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
			} catch (OpenR66ProtocolPacketException e2) {
			}
			try {
				session.setFinalizeTransfer(false, new R66Result(e, session, true, runner.getErrorInfo(), runner));
			} catch (OpenR66RunnerErrorException e1) {
				localChannelReference.invalidateRequest(new R66Result(e, session, true, runner.getErrorInfo(), runner));
			} catch (OpenR66ProtocolSystemException e1) {
				localChannelReference.invalidateRequest(new R66Result(e, session, true, runner.getErrorInfo(), runner));
			}
			session.setStatus(97);
			ChannelCloseTimer.closeFutureChannel(channel);
			return;
		}
		// Success: No write back at all
	}
}
