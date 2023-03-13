/*
 *    _           _             _   _
 *   (_) __ _  __| | __ _ _ __ | |_(_)_   _____
 *   | |/ _` |/ _` |/ _` | '_ \| __| \ \ / / _ \
 *   | | (_| | (_| | (_| | |_) | |_| |\ V /  __/
 *  _/ |\__,_|\__,_|\__,_| .__/ \__|_| \_/ \___|
 * |__/                  |_|
 *
 * This file is part of the Maverick Synergy Hotfixes Java SSH API
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * Copyright (C) 2002-2021 JADAPTIVE Limited - All Rights Reserved
 *
 * Use of this software may also be covered by third-party licenses depending on the choices you make about what features to use.
 *
 * Please visit the link below to see additional third-party licenses and copyrights
 *
 * https://www.jadaptive.com/app/manpage/en/article/1565029/What-third-party-dependencies-does-the-Maverick-Synergy-API-have
 */
package com.sshtools.common.events;

/**
 * List of common event codes
 */
public class EventCodes {

		public static final String ATTRIBUTE_IP="IP";
	
		/** This attribute is the key for the log message previously passes to Log.info()/Log.debug() calls*/
		public static final String ATTRIBUTE_LOG_MESSAGE="LOG_MESSAGE";
	
		/** This attribute is the key for the throwable object previously passed in some Log.info() calls.*/
		public static final String ATTRIBUTE_THROWABLE="THROWABLE";

		/** The current connection */
    	public static final String ATTRIBUTE_CONNECTION = "CONNECTION";

	    /** The remote ip */
		public static final String ATTRIBUTE_REMOTE_IP = "REMOTE_IP";
	
		public static final String ATTRIBUTE_HOST_KEY="HOST_KEY";
		public static final String ATTRIBUTE_HOST_PUBLIC_KEY="HOST_PUBLIC_KEY";
		public static final String ATTRIBUTE_CLIENT="CLIENT";
		public static final String ATTRIBUTE_SESSION="SESSION";
			
		public static final String ATTRIBUTE_REMOTE_IDENT = "REMOTE_IDENT";
		public static final String ATTRIBUTE_LOCAL_IDENT = "LOCAL_IDENT";
		
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_USING_KEY_EXCHANGE="USING_KEY_EXCHANGE";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_USING_PUBLICKEY="USING_PUBLICKEY";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_USING_CS_CIPHER="USING_CS_CIPHER";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_USING_SC_CIPHER="USING_SC_CIPHERC";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_USING_CS_MAC="USING_CS_MAC";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_USING_SC_MAC="USING_SC_MAC";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_USING_CS_COMPRESSION="USING_CS_COMPRESSION";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_USING_SC_COMPRESSION="USING_SC_COMPRESSION";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_REMOTE_KEY_EXCHANGES="REMOTE_KEY_EXCHANGES";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_REMOTE_PUBLICKEYS="REMOTE_PUBLICKEYS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_REMOTE_CIPHERS_CS="REMOTE_CIPHERS_CS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_REMOTE_CIPHERS_SC="REMOTE_CIPHERS_SC";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_REMOTE_CS_MACS="REMOTE_CS_MACS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_REMOTE_SC_MACS="REMOTE_SC_MACS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_REMOTE_CS_COMPRESSIONS="REMOTE_CS_COMPRESSIONS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_REMOTE_SC_COMPRESSIONS="REMOTE_SC_COMPRESSIONS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_LOCAL_KEY_EXCHANGES="LOCAL_KEY_EXCHANGES";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_LOCAL_PUBLICKEYS="LOCAL_PUBLICKEYS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_LOCAL_CIPHERS_CS="LOCAL_CIPHERS_CS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_LOCAL_CIPHERS_SC="LOCAL_CIPHERS_SC";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_LOCAL_CS_MACS="LOCAL_CS_MACS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_LOCAL_SC_MACS="LOCAL_SC_MACS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_LOCAL_CS_COMPRESSIONS="LOCAL_CS_COMPRESSIONS";
		/** algorithm negotiation preferences*/
		public static final String ATTRIBUTE_LOCAL_SC_COMPRESSIONS="LOCAL_SC_COMPRESSIONS";
		/** Open directory handles on SFTP close **/
		public static final String ATTRIBUTE_OPEN_DIRECTORY_HANDLES = "OPEN_DIR_HANDLES";
		/** Open file handles on SFTP close **/
		public static final String ATTRIBUTE_OPEN_FILE_HANDLES = "OPEN_FILE_HANDLES";
		
		public static final String ATTRIBUTE_AUTHENTICATION_METHODS="AUTHENTICATION_METHODS";
		public static final String ATTRIBUTE_AUTHENTICATION_METHOD = "AUTHENTICATION_METHOD";
		
		public static final String ATTRIBUTE_FORWARDING_TUNNEL_ENTRANCE="FORWARDING_TUNNEL_ENTRANCE";
		public static final String ATTRIBUTE_FORWARDING_TUNNEL_EXIT="FORWARDING_TUNNEL_EXIT";
		
		public static final String ATTRIBUTE_FILE_NAME="FILE_NAME";
		public static final String ATTRIBUTE_FILE_NEW_NAME="FILE_NEW_NAME";
		public static final String ATTRIBUTE_FILE_TARGET = "FILE_TARGET";
		public static final String ATTRIBUTE_DIRECTORY_PATH="DIRECTORY_PATH";
		public static final String ATTRIBUTE_COMMAND="COMMAND";
		
		public static final String ATTRIBUTE_NUMBER_OF_CONNECTIONS="NUMBER_OF_CONNECTIONS";
		
		public static final String ATTRIBUTE_LOCAL_COMPONENT_LIST="LOCAL_COMPONENT_LIST";
		public static final String ATTRIBUTE_REMOTE_COMPONENT_LIST="REMOTE_COMPONENT_LIST";

		public static final String ATTRIBUTE_ATTEMPTED_USERNAME = "USERNAME";
		
		public static final String ATTRIBUTE_BYTES_READ = "BYTES_READ";
		public static final String ATTRIBUTE_BYTES_WRITTEN = "BYTES_WRITTEN";
		public static final String ATTRIBUTE_BYTES_TRANSFERED = "BYTES_TRANSFERED";
		public static final String ATTRIBUTE_BYTES_EXPECTED = "BYTES_EXPECTED";
		public static final String ATTRIBUTE_HANDLE = "HANDLE";
		
		public static final String ATTRIBUTE_OPERATION_STARTED = "OP_STARTED";
		public static final String ATTRIBUTE_OPERATION_FINISHED = "OP_FINISHED";
		
		public static final String ATTRIBUTE_OLD_ATTRIBUTES = "OLD_ATTR";
		public static final String ATTRIBUTE_NEW_ATRTIBUTES = "NEW_ATTR";
		public static final String ATTRIBUTE_ATRTIBUTES = "ATTR";
		
		public static final String ATTRIBUTE_ABSTRACT_FILE = "ABSTRACT_FILE";
		public static final String ATTRIBUTE_ABSTRACT_FILE_INPUTSTREAM = "ABSTRACT_FILE_IN";
		public static final String ATTRIBUTE_ABSTRACT_FILE_OUTPUTSTREAM = "ABSTRACT_FILE_OUT";
		public static final String ATTRIBUTE_ABSTRACT_FILE_RANDOM_ACCESS = "ABSTRACT_FILE_RAF";
		
		public static final String ATTRIBUTE_FILE_FACTORY = "FILE_FACTORY";
		public static final String ATTRIBUTE_MOUNT_MANAGER = "MOUNT_MANAGER";

		
		/** Connection attempt **/
		public static final int EVENT_CONNECTION_ATTEMPT = 			0xFF000000;
		public static final int EVENT_CONNECTED = 					0xFF000001;
		public static final int EVENT_NEGOTIATED_PROTOCOL = 		0xFF00000A;
		// Not very useful events
		public static final int EVENT_HOSTKEY_RECEIVED	=			0xFF000002; 
		public static final int EVENT_HOSTKEY_REJECTED 	=			0xFF000003;
		public static final int EVENT_HOSTKEY_ACCEPTED  =   		0xFF000004;

		public static final int EVENT_KEY_EXCHANGE_INIT	= 			0xFF000005;
		public static final int EVENT_KEY_EXCHANGE_FAILURE =		0xFF000006;
		public static final int EVENT_KEY_EXCHANGE_COMPLETE =		0xFF000007;
		
		public static final int EVENT_FAILED_TO_NEGOTIATE_TRANSPORT_COMPONENT = 0xFF000008;
		
		// Useful events
		public static final int EVENT_AUTHENTICATION_STARTED =		0xFF000014;
		public static final int EVENT_USERAUTH_STARTED = 			0xFF000013;
		public static final int EVENT_USERAUTH_SUCCESS = 			0xFF000010;
		public static final int EVENT_USERAUTH_FAILURE =  			0xFF000011;
		public static final int EVENT_AUTHENTICATION_COMPLETE = 	0xFF000012;
		

		// Session events
		public static final int EVENT_FORWARDING_REMOTE_STARTED =	0xFF000030;
		public static final int EVENT_FORWARDING_REMOTE_STOPPED =	0xFF000031;
		public static final int EVENT_SHELL_SESSION_STARTED =		0xFF000032;
		public static final int EVENT_SHELL_COMMAND = 				0xFF000033;
		
		// SCP Events
		public static final int EVENT_SCP_UPLOAD_STARTED 	= 		0xFF000042;
		public static final int EVENT_SCP_DOWNLOAD_STARTED = 		0xFF000043;
		public static final int EVENT_SCP_UPLOAD_COMPLETE = 		0xFF000040;
		public static final int EVENT_SCP_DOWNLOAD_COMPLETE =		0xFF000041;
		public static final int EVENT_SCP_FILE_READ = 				0xFF000044;
		public static final int EVENT_SCP_FILE_WRITE = 				0xFF000045;
		public static final int EVENT_SCP_UPLOAD_INIT 	= 			0xFF000046;
		public static final int EVENT_SCP_DOWNLOAD_INIT = 			0xFF000047;
		
		// SFTP Events
		public static final int EVENT_SFTP_SESSION_STARTED =		0xFF000050;
		public static final int EVENT_SFTP_SESSION_STOPPED =		0xFF000051;
		public static final int EVENT_SFTP_DIRECTORY_CREATED = 		0xFF000052;
		public static final int EVENT_SFTP_DIRECTORY_DELETED =		0xFF000053;
		public static final int EVENT_SFTP_FILE_RENAMED =			0xFF000054;
		public static final int EVENT_SFTP_FILE_DELETED	=			0xFF000055;
		public static final int EVENT_SFTP_SYMLINK_CREATED = 		0xFF000056;
		public static final int EVENT_SFTP_FILE_UPLOAD_COMPLETE = 	0xFF000057;
		public static final int EVENT_SFTP_FILE_DOWNLOAD_COMPLETE = 0xFF000058;
		public static final int EVENT_SFTP_FILE_TOUCHED = 			0xFF000059;
		public static final int EVENT_SFTP_FILE_ACCESS = 			0xFF00005A;
		public static final int EVENT_SFTP_FILE_ACCESS_COMPLETE = 	0xFF00005A;
		public static final int EVENT_SFTP_SET_ATTRIBUTES = 		0xFF00005B;
		public static final int EVENT_SFTP_DIR = 					0xFF00005C;
		public static final int EVENT_SFTP_FILE_DOWNLOAD_STARTED =  0xFF00005D;
		public static final int EVENT_SFTP_FILE_UPLOAD_STARTED   =  0xFF00005E;
		public static final int EVENT_SFTP_FILE_ACCESS_STARTED = 	0xFF00005F;
		public static final int EVENT_SFTP_FILE_READ			 = 	0xFF000060;
		public static final int EVENT_SFTP_FILE_WRITE			 = 	0xFF000061;
		public static final int EVENT_SFTP_SESSION_STOPPING = 		0xFF000062;
		
		public static final int EVENT_SFTP_FILE_DOWNLOAD_INIT =		0xFF000063;
		public static final int EVENT_SFTP_FILE_UPLOAD_INIT   =  	0xFF000064;
		public static final int EVENT_SFTP_FILE_ACCESS_INIT	  =  	0xFF000065;
		
		public static final int EVENT_SFTP_GET_ATTRIBUTES = 		0xFF000066;
		public static final int EVENT_SFTP_DIRECTORY_OPENED =		0xFF000067;
		
		// System Events
		public static final int EVENT_REACHED_CONNECTION_LIMIT = 	0xFF0000A0;

		public static final int EVENT_REMOTE_DISCONNECTED = 		0xFF0000FE;
		public static final int EVENT_DISCONNECTED =				0xFF0000FF;

		// Shell events
		public static final int EVENT_ROOT_SHELL_STARTED = 			0xFF000080;
		public static final int EVENT_ROOT_SHELL_STOPPED = 			0xFF000081;
		
		public static final int EVENT_AUTHENTICATION_METHODS_RECEIVED =	0x00FF0011;
		
		public static final int EVENT_USERAUTH_FURTHER_AUTHENTICATION_REQUIRED = 0x00FF0015;
		
		public static final int EVENT_FORWARDING_LOCAL_STARTED = 	0x00FF0016;
		public static final int EVENT_FORWARDING_LOCAL_STOPPED = 	0x00FF0018;
		
		public static final int EVENT_RECEIVED_DISCONNECT = 		0x00FF0021;

		public static final int EVENT_SHELL_SESSION_FAILED_TO_START=0x00FF0024;
		public static final int EVENT_SUBSYSTEM_STARTED = 			0x00FF1001;
		public static final int EVENT_SFTP_FILE_CLOSED = 			0x00FF0025;
		public static final int EVENT_SFTP_FILE_OPENED = 			0x00FF0026;

		public static final String ATTRIBUTE_REASON = "REASON";

		

}
