package com.sshtools.common.policy;

/*-
 * #%L
 * Base API
 * %%
 * Copyright (C) 2002 - 2024 JADAPTIVE Limited
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.permissions.Permissions;
import com.sshtools.common.scp.ScpPolicy;
import com.sshtools.common.scp.ScpPolicy.ScpPolicyBuilder;
import com.sshtools.common.sftp.SftpExtension;
import com.sshtools.common.sftp.SftpExtensionFactory;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.util.IOUtils;
import com.sshtools.common.util.UnsignedInteger32;

/**
 * Represents various file system related policy.
 * 
 * Note, will be made <code>final</code> at version 3.3.0, and will only be able to
 * be constructed using the {@link FileSystemPolicyBuilder}.
 * 
 * TODO make final at 3.3.0+
 */
public class FileSystemPolicy extends Permissions {
	
	/**
	 * Build a new {@link FileSystemPolicy}.
	 */
	public final static class FileSystemPolicyBuilder extends AbstractPermissionBuilder<Permission, FileSystemPolicyBuilder> {
		
		private boolean readWriteEvents;
		private Charset charsetEncoding = Charset.forName("UTF-8");
		private Optional<Long> connectionUploadQuota = Optional.empty();
		private boolean allowZeroLengthFileUpload = true;
		private int sftpVersion = 4;
		private int maxConcurrentTransfers = 50;
		private int sftpMaxPacketSize = 65536;
		private long sftpMaxWindowSize = IOUtils.fromByteSize("16MB").longValue();
		private long sftpMinWindowSize = 131072;
		private boolean mkdirParentMustExist  = true;
		private boolean closeFileBeforeFailedTransferEvents = false;
		private String sftpLongnameDateFormat = "MMM dd  yyyy";
		private String sftpLongnameDateFormatWithTime = "MMM dd HH:mm";
		private List<SftpExtensionFactory> sftpExtensionFactories = new ArrayList<>();
		private Optional<Predicate<SftpExtension>> sftpExtensionFilter = Optional.empty();

		private FileSystemPolicyBuilder() { }
		
		/**
		 * Create a new {@link FileSystemPolicyBuilder} that will be used to configure
		 * and create a {@link FileSystemPolicy}.
		 * 
		 * @return builder
		 */
		public static FileSystemPolicyBuilder create() {
			return new FileSystemPolicyBuilder(); 
		}

		/**
		 * Set a filter that can be used to determine if an SFTP extension should be used. If the
		 * {@link Predicate} returns <code>true</code>, the extension can be used. When there is no
		 * filter, all extensions are allowed.
		 *  
		 * @param filter file
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withSftpExtensionFilter(Predicate<SftpExtension> sftpExtensionFilter) {
			this.sftpExtensionFilter = Optional.of(sftpExtensionFilter);
			return this;
		}

		/**
		 * Set the list of {@link SftpExtensionFactory}s that can be used to handle
		 * custom SFTP messages.
		 *  
		 * @param sftpExtensionFactories factorys
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withSftpExtensionFactories(SftpExtensionFactory... sftpExtensionFactories) {
			return withSftpExtensionFactories(Arrays.asList(sftpExtensionFactories));
		}

		/**
		 * Set the list of {@link SftpExtensionFactory}s that can be used to handle
		 * custom SFTP messages.
		 *  
		 * @param sftpExtensionFactories factorys
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withSftpExtensionFactories(Collection<SftpExtensionFactory> sftpExtensionFactories) {
			this.sftpExtensionFactories.clear();
			return addSftpExtensionFactories(sftpExtensionFactories);
		}

		/**
		 * Add to the list of {@link SftpExtensionFactory}s that can be used to handle
		 * custom SFTP messages.
		 *  
		 * @param sftpExtensionFactories factorys
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder addSftpExtensionFactories(SftpExtensionFactory... sftpExtensionFactories) {
			return addSftpExtensionFactories(sftpExtensionFactories);
		}

		/**
		 * Add to the list of {@link SftpExtensionFactory}s that can be used to handle
		 * custom SFTP messages.
		 *  
		 * @param sftpExtensionFactories factorys
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder addSftpExtensionFactories(Collection<SftpExtensionFactory> factories) {
			this.sftpExtensionFactories.addAll(factories);
			return this;
		}

		/**
		 * Set to close the file before any failed transfer events are fired.
		 *  
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withCloseFileBeforeFailedTransferEvents() {
			return withCloseFileBeforeFailedTransferEvents(true);
		}

		/**
		 * Set the whether to close the file before any failed transfer events are fired.
		 *  
		 * @param closeFileBeforeFailedTransferEvents close file.
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withCloseFileBeforeFailedTransferEvents(boolean closeFileBeforeFailedTransferEvents) {
			this.closeFileBeforeFailedTransferEvents = closeFileBeforeFailedTransferEvents;
			return this;
		}

		/**
		 * Set that parent directories do not need to exist on directory creation operations, any
		 * parents will be automatically created.
		 *  
		 * @param mkdirParentMustExist parent must exist.
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withoutMkdirParentMustExists() {
			return withMkdirParentMustExists(false);
		}

		/**
		 * Set the whether the parent must exist for any directory creation operation to complete.
		 *  
		 * @param mkdirParentMustExist parent must exist.
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withMkdirParentMustExists(boolean mkdirParentMustExist) {
			this.mkdirParentMustExist = mkdirParentMustExist;
			return this;
		}

		/**
		 * Set the maximum number of concurrent transfers.
		 *  
		 * @param maxConcurrentTransfers maximum number of concurrent transfers.
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withMaxConcurrentTransfers(int maxConcurrentTransfers) {
			this.maxConcurrentTransfers = maxConcurrentTransfers;
			return this;
		}

		/**
		 * Set the supported SFTP version.
		 *  
		 * @param sftpVersion sftp version
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withSftpVersion(int sftpVersion) {
			this.sftpVersion = sftpVersion;
			return this;
		}
		
		/**
		 * Set whether zero length file uploads are allowed.
		 * 
		 * @param allowZeroLengthFileUpload allow zero length file upload
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withZeroLengthFileUploads(boolean allowZeroLengthFileUpload) {
			this.allowZeroLengthFileUpload = allowZeroLengthFileUpload;
			return this;
		}
		
		/**
		 * Enable read / write events for SFTP file transfers
		 * 
		 * @param readWriteEvents enable read / write events
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withReadWriteEvents() {
			return withReadWriteEvents(true);
		}
		
		/**
		 * Set whether read / write events are enabled for SCP file transfers
		 * 
		 * @param readWriteEvents enable read / write events
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withReadWriteEvents(boolean readWriteEvents) {
			this.readWriteEvents = readWriteEvents;
			return this;
		}
		
		/**
		 * Set the character set encoding used for SCP (filenames etc)
		 * 
		 * @param charsetEncoding character set encoding
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withCharsetEncoding(Charset charsetEncoding) {
			this.charsetEncoding  = charsetEncoding;
			return this;
		}
		
		/**
		 * Set the character set encoding used for SCP (filenames etc)
		 * 
		 * @param charsetEncoding character set encoding
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withCharsetEncoding(String charsetEncoding) {
			return withCharsetEncoding(Charset.forName(charsetEncoding));
		}
		
		/**
		 * Set a per-connection upload quota
		 * 
		 * @param connectionUploadQuota connection upload quota
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withConnectionUploadQuota(long connectionUploadQuote) {
			this.connectionUploadQuota = connectionUploadQuote == -1 ? Optional.empty() : Optional.of(connectionUploadQuote);
			return this;
		}
		
		/**
		 * Set the maximum sftp packet size in bytes.
		 * 
		 * @param sftpMaxPacketSize sftp max packet size
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withSftpMaxPacketSize(int sftpMaxPacketSize) {
			this.sftpMaxPacketSize = sftpMaxPacketSize;
			return this;
		}
		/**
		 * Set the maximum sftp window size in bytes.
		 * 
		 * @param sftpMaxWindowSize maximum sftp window size
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withSftpMaxWindowSize(long sftpMaxWindowSize) {
			this.sftpMaxWindowSize = sftpMaxWindowSize;
			return this;
		}
		
		/**
		 * Set the maximum sftp window size in bytes.
		 * 
		 * @param sftpMaxWindowSize maximum sftp window size
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withSftpMaxWindowSize(UnsignedInteger32 sftpMaxWindowSize) {
			return withSftpMaxWindowSize(sftpMaxWindowSize.longValue());
		}
		
		/**
		 * Set the minimum sftp window size in bytes.
		 * 
		 * @param sftpMinWindowSize sftp session window size
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withSftpMinWindowSize(long sftpMinWindowSize) {
			this.sftpMinWindowSize = sftpMinWindowSize;
			return this;
		}
		
		/**
		 * Set the minimum sftp window size in bytes.
		 * 
		 * @param sftpMinWindowSize maximum sftp window size
		 * @return this for chaining
		 */
		public FileSystemPolicyBuilder withSftpMinWindowSize(UnsignedInteger32 sftpMinWindowSize) {
			return withSftpMinWindowSize(sftpMinWindowSize.longValue());
		}
		
		/**
		 * Build a new {@link ScpPolicy} given the configuration of this builder.
		 * 
		 * @return policy
		 */
		public FileSystemPolicy build() {
			return new FileSystemPolicy(this);
		}
	}

	/* TODO make all of these private + final, remove all deprecated setters at 3.3.x+ */
	private Optional<Long> connectionUploadQuota;
	FileFactory fileFactory;
	private Charset sftpCharsetEncoding;
	private boolean allowZeroLengthFileUpload;
	private int sftpVersion;
	private boolean sftpReadWriteEvents;
	private int maxConcurrentTransfers;
	private String sftpLongnameDateFormat;
	private String sftpLongnameDateFormatWithTime;
	private List<SftpExtensionFactory> sftpExtensionFactories;
	private Set<String> disabledExtensions = new HashSet<>();
	private boolean closeFileBeforeFailedTransferEvents;
	private boolean mkdirParentMustExist;
	
	private int sftpMaxPacketSize = 65536;
	private UnsignedInteger32 sftpMaxWindowSize = new UnsignedInteger32(IOUtils.fromByteSize("16MB").longValue());
	private UnsignedInteger32 sftpMinWindowSize = new UnsignedInteger32(131072);
	private final Optional<Predicate<SftpExtension>> sftpExtensionFilter;

	/**
	 * Construct a new policy
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public FileSystemPolicy() {
		sftpCharsetEncoding = Charset.forName("UTF-8");
		sftpReadWriteEvents = false;
		connectionUploadQuota = Optional.empty();
		allowZeroLengthFileUpload = true;
		sftpVersion = 4;
		maxConcurrentTransfers = 50;
		sftpMaxPacketSize = 65536;
		sftpMaxWindowSize = new UnsignedInteger32(IOUtils.fromByteSize("16MB").longValue());
		sftpMinWindowSize = new UnsignedInteger32(131072);
		mkdirParentMustExist = true;
		closeFileBeforeFailedTransferEvents = false;
		sftpLongnameDateFormat = "MMM dd  yyyy";
		sftpLongnameDateFormatWithTime = "MMM dd HH:mm";
		sftpExtensionFactories = new ArrayList<SftpExtensionFactory>();
		sftpExtensionFilter = Optional.of(fact -> !disabledExtensions.contains(fact.getName()));
	}
	
	private FileSystemPolicy(FileSystemPolicyBuilder bldr) {
		super(bldr);
		sftpCharsetEncoding = bldr.charsetEncoding;
		sftpReadWriteEvents = bldr.readWriteEvents;
		connectionUploadQuota = bldr.connectionUploadQuota;
		allowZeroLengthFileUpload = bldr.allowZeroLengthFileUpload;
		sftpVersion = bldr.sftpVersion;
		maxConcurrentTransfers = bldr.maxConcurrentTransfers;
		sftpMaxPacketSize = bldr.sftpMaxPacketSize;
		sftpMaxWindowSize = new UnsignedInteger32(bldr.sftpMaxWindowSize);
		sftpMinWindowSize = new UnsignedInteger32(bldr.sftpMinWindowSize);
		mkdirParentMustExist = bldr.mkdirParentMustExist;
		closeFileBeforeFailedTransferEvents = bldr.closeFileBeforeFailedTransferEvents;
		sftpLongnameDateFormat = bldr.sftpLongnameDateFormat;
		sftpLongnameDateFormatWithTime = bldr.sftpLongnameDateFormatWithTime;
		sftpExtensionFactories = Collections.unmodifiableList(new ArrayList<SftpExtensionFactory>(bldr.sftpExtensionFactories));
		sftpExtensionFilter = bldr.sftpExtensionFilter;
	}
	
	/**
	 * Get the optional connection upload quota.
	 * 
	 * @return quota
	 */
	public Optional<Long> connectionUploadQuota() {
		return connectionUploadQuota;
	}

	/**
	 * Get the connection upload quota, or <code>-1</code> if there is none.
	 * 
	 * @return connection upload quota
	 */
	public long getConnectionUploadQuota() {
		return connectionUploadQuota.orElse(-1l);
	}
	
	/**
	 * Get the connection upload quota, or <code>-1</code> if there is none.
	 * 
	 * @param connectionUploadQuota connection upload quota
	 * @deprecated will become immutable, use {@link FileSystemPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setConnectionUploadQuota(long connectionUploadQuota) {
		this.connectionUploadQuota = connectionUploadQuota == -1 ? Optional.empty() : Optional.of(connectionUploadQuota);
	}
	
	/**
	 * Get whether there is a connection upload quota.
	 * 
	 * @return connection upload quota exists
	 */
	public boolean hasUploadQuota() {
		return connectionUploadQuota.isPresent();
	}
	
	/**
	 * Get the current encoding value for filenames in SFTP sessions.
	 * 
	 * @return charset encoding
	 * @deprecated see {@link #scpCharsetEncoding()}
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public String getSFTPCharsetEncoding() {
		return sftpCharsetEncoding.name();
	}
	
	/**
	 * Get the current encoding value for filenames in SFTP sessions.
	 * 
	 * @return charset encoding
	 */
	public Charset sftpCharsetEncoding() {
		return sftpCharsetEncoding;
	}

	/**
	 * Set the default encoding for filenames in SFTP sessions. The default
	 * encoding for the currently supported SFTP protocol is ISO-8859-1.
	 * 
	 * @param sftpCharsetEncoding encoding
	 * @deprecated will become immutable, use {@link FileSystemPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSFTPCharsetEncoding(String sftpCharsetEncoding) {
		this.sftpCharsetEncoding = Charset.forName(sftpCharsetEncoding);
	}
	
	/**
	 * Set the file factory for this context.
	 * @param fileFactory
	 */
	public void setFileFactory(FileFactory fileFactory) {
		this.fileFactory = new CachingFileFactory(fileFactory);
	}
	
	/**
	 * Get the file factory for this context.
	 * @return
	 * @throws PermissionDeniedException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public FileFactory getFileFactory() {
		return fileFactory;
	}
	
	/**
	 * Get whether zero length file uploads are allowed.
	 *  
	 * @return allow zero length file upload
	 */
	public boolean isAllowZeroLengthFileUpload() {
		return allowZeroLengthFileUpload;
	}

	/**
	 * Set whether zero length file uploads are allowed.
	 *  
	 * @param allowZeroLengthFileUpload allow zero length file upload
	 * @deprecated will become immutable, use {@link FileSystemPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setAllowZeroLengthFileUpload(boolean allowZeroLengthFileUpload) {
		this.allowZeroLengthFileUpload = allowZeroLengthFileUpload;
	}

	/**
	 * Set the maximum number of concurrent transfers.
	 *  
	 * @param maxConcurrentTransfers maximum number of concurrent transfers.
	 * @deprecated will become immutable, use {@link FileSystemPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setMaxConcurrentTransfers(int maxConcurrentTransfers) {
		this.maxConcurrentTransfers = maxConcurrentTransfers;
	}

	/**
	 * Get the maximum number of concurrent transfers.
	 *  
	 * @return maximum number of concurrent transfers.
	 */
	public int getMaxConcurrentTransfers() {
		return maxConcurrentTransfers;
	}

	/**
	 * Set the supported SFTP version.
	 *  
	 * @param sftpVersion sftp version
	 * @deprecated will become immutable, use {@link FileSystemPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSupportedSFTPVersion(int sftpVersion) {
		if(sftpVersion < 1 || sftpVersion > 4) {
			throw new IllegalArgumentException("SFTP version must be between 1 and 4");
		}
		this.sftpVersion = sftpVersion;
	}
	
	/**
	 * @return sftp version
	 * @deprecated case change for consistency, see {@link #getSftpVersion()}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public int getSFTPVersion() {
		return sftpVersion;
	}
	
	/**
	 * Get the supported SFTP version.
	 * 
	 * @return supported SFTP version
	 */
	public final int getSftpVersion() {
		return sftpVersion;
	}
	
	/**
	 * Set whether read / write events are fired during SFTP operations
	 * 
	 * @param sftpReadWriteEvents read / write events fired
	 * @deprecated will become immutable, use {@link FileSystemPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSFTPReadWriteEvents(boolean sftpReadWriteEvents) {
		this.sftpReadWriteEvents = sftpReadWriteEvents;
	}
	
	/**
	 * @return sftp version
	 * @deprecated case change for consistency, see {@link #getSftpVersion()}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public boolean isSFTPReadWriteEvents() {
		return sftpReadWriteEvents;
	}
	
	/**
	 * Get whether read / write events are fired during SFTP operations.
	 * 
	 * @return read / write events
	 */
	public final boolean isSftpReadWriteEvents() {
		return sftpReadWriteEvents;
	}

	/**
	 * NOTE. This method never worked and will now throw an exception.
	 *  
	 * @param scpReadWriteEvents SCP read events
	 * @deprecated will become immutable, use {@link ScpPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSCPReadWriteEvents(boolean scpReadWriteEvents) {
		throw new UnsupportedOperationException("Use " + ScpPolicy.class.getName());
	}

	@Deprecated(since = "3.2.0", forRemoval = true)
	public boolean isSCPReadWriteEvents() {
		return false;
	}
	
	/**
	 * @deprecated unused.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public int getMaximumNumberOfAsyncSFTPRequests() {
		return 0;
	}
	
	/**
	 * @deprecated unused.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setMaximumNumberofAsyncSFTPRequests(int maximumSftpRequests) {
	}

	/**
	 * Get the longname date format (used for SFTP < 3)
	 * 
	 * @return sftp longname date format
	 * @deprecated renamed for consistency, see {@link #getSftpLongnameDateFormat()}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public String getSFTPLongnameDateFormat() {
		return sftpLongnameDateFormat; //"MMM dd yyyy";
	}
	
	/**
	 * Get the longname date format (used for SFTP < 3)
	 * 
	 * @return sftp longname date format
	 */
	public String getSftpLongnameDateFormat() {
		return sftpLongnameDateFormat; //"MMM dd yyyy";
	}

	/**
	 * Get the longname date with time format (used for SFTP < 3)
	 * 
	 * @return sftp longname date with time format
	 * @deprecated renamed for consistency, see {@link #getSftpLongnameDateFormatWithTime()}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public String getSFTPLongnameDateFormatWithTime() {
		return sftpLongnameDateFormatWithTime; //"MMM dd HH:mm";
	}
	
	/**
	 * Get the longname date with time format (used for SFTP < 3)
	 * 
	 * @return sftp longname date with time format
	 */
	public String getSftpLongnameDateFormatWithTime() {
		return sftpLongnameDateFormatWithTime; //"MMM dd HH:mm";
	}
	
	/**
	 * Prevents an extension for the given request name from being used. Note, this will not
	 * work if {@link FileSystemPolicyBuilder#withSftpExtensionFilter(Predicate)} has been used.
	 * 
	 * @param requestName request name
	 * @deprecated replaced with {@link FileSystemPolicyBuilder#withSftpExtensionFilter(Predicate)}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void disableSFTPExtension(String requestName) {
		disabledExtensions.add(requestName);
	}

	/**
	 * Re-enabled a previously disabled extension. Note, this will not
	 * work if {@link FileSystemPolicyBuilder#withSftpExtensionFilter(Predicate)} has been used.
	 * 
	 * @param requestName request name
	 * @deprecated replaced with {@link FileSystemPolicyBuilder#withSftpExtensionFilter(Predicate)}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void enableSFTPExtension(String requestName) {
		disabledExtensions.remove(requestName);
	}

	/**
	 * Get an extension given its request name. The extension must be allowed if a 
	 * {@link FileSystemPolicyBuilder#withSftpExtensionFilter(Predicate)} has been used.
	 * If no factory supports the request name, or if the filter indicates the extension may
	 * not be used, {@link Optional#isEmpty()} will be <code>true</code> 
	 * 
	 * @param requestName request name
	 * @return optional extension
	 */
	public Optional<SftpExtension> sftpExtension(String requestName) {
		return Optional.ofNullable(getSFTPExtension(requestName));
	}

	/**
	 * Get an extension given its request name. The extension must be allowed if a 
	 * {@link FileSystemPolicyBuilder#withSftpExtensionFilter(Predicate)} has been used.
	 * If no factory supports the request name, or if the filter indicates the extension may
	 * not be used, <code>null</code> will be returned.
	 * 
	 * @param requestName request name
	 * @return optional extension
	 * @deprecated replaced with {@link #sftpExtension(String)}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public SftpExtension getSFTPExtension(String requestName) {
		if(disabledExtensions.contains(requestName)) {
			return null;
		}
		for(SftpExtensionFactory factory : sftpExtensionFactories) {
			if(factory.getSupportedExtensions().contains(requestName)) {
				SftpExtension extension = factory.getExtension(requestName);
				if(sftpExtensionFilter.map(p -> p.test(extension)).orElse(true)) {
					return extension;
				}
			}
		}
		return null;
	}

	/**
	 * Get all extension factories. Note, from version 3.2.0 this becomes immutable.
	 * 
	 * @return sftp extension factories
	 */
	public Collection<SftpExtensionFactory> getSFTPExtensionFactories() {
		return sftpExtensionFactories;
	}
	
	/**
	 * Get whether to close files before failed transfer events.
	 * 
	 * @param closeFileBeforeFailedTransferEvents close files before failed transfer events
	 * @deprecated renamed for consistency, see {@link #isCloseFileBeforeFailedTransferEvents()}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public boolean isSFTPCloseFileBeforeFailedTransferEvents() {
		return closeFileBeforeFailedTransferEvents;
	}
	
	/**
	 * Get whether to close files before failed transfer events.
	 * 
	 * @return close files before failed transfer events
	 */
	public boolean isCloseFileBeforeFailedTransferEvents() {
		return closeFileBeforeFailedTransferEvents;
	}
	
	/**
	 * Set whether to close files before failed transfer events.
	 * 
	 * @param closeFileBeforeFailedTransferEvents close files before failed transfer events
	 * @deprecated will become immutable, use {@link ScpPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSFTPCloseFileBeforeFailedTransferEvents(boolean closeFileBeforeFailedTransferEvents) {
		this.closeFileBeforeFailedTransferEvents = closeFileBeforeFailedTransferEvents;
	}
	
	/**
	 * Get the maximum SFTP packet size
	 * 
	 * @return maximum SFTP packet size
	 */
	public int getSftpMaxPacketSize() {
		return sftpMaxPacketSize;
	}

	/**
	 * Set the maximum SFTP packet size
	 * 
	 * @param sftpMaxPacketSize maximum SFTP packet size
	 * @deprecated will become immutable, use {@link ScpPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSftpMaxPacketSize(int sftpMaxPacketSize) {
		this.sftpMaxPacketSize = sftpMaxPacketSize;
	}
	
	/**
	 * Get the maximum SFTP window size
	 * 
	 * @return maximum SFTP window size
	 */
	public UnsignedInteger32 getSftpMaxWindowSize() {
		return sftpMaxWindowSize;
	}

	/**
	 * Set the maximum SFTP window size
	 * 
	 * @param sftpMaxWindowSize maximum SFTP window size
	 * @deprecated will become immutable, use {@link ScpPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSftpMaxWindowSize(UnsignedInteger32 sftpMaxWindowSize) {
		this.sftpMaxWindowSize = sftpMaxWindowSize;
	}
	
	/**
	 * Get the minimum SFTP window size
	 * 
	 * @return minimum SFTP window size
	 */
	public UnsignedInteger32 getSftpMinWindowSize() {
		return sftpMinWindowSize;
	}
	
	/**
	 * Set the minimum SFTP window size
	 * 
	 * @param sftpMinWindowSize minimum SFTP window size
	 * @deprecated will become immutable, use {@link ScpPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setSftpMinWindowSize(UnsignedInteger32 sftpMinWindowSize) {
		this.sftpMinWindowSize = sftpMinWindowSize;
	}
	
	class CachingFileFactory implements FileFactory {

		private static final String CACHED_FILE_FACTORY = "cachedFileFactory";
		
		FileFactory fileFactory;
		
		CachingFileFactory(FileFactory fileFactory) {
			this.fileFactory = fileFactory;
		}
		
		@Override
		public AbstractFileFactory<?> getFileFactory(SshConnection con) 
				throws IOException, PermissionDeniedException {
			AbstractFileFactory<?> ff = (AbstractFileFactory<?>) con.getProperty(CACHED_FILE_FACTORY);
			if(Objects.isNull(ff)) {
				if(Objects.isNull(fileFactory)) {
					throw new PermissionDeniedException("Invalid file system configuration");
				}
				ff = fileFactory.getFileFactory(con);
				con.setProperty(CACHED_FILE_FACTORY, ff);
			}
			return ff;
		}
		
	}
	
	/**
	 * Set whether the parent must exist for any directory creation operation to complete.
	 * 
	 * @param mkdirParentMustExist parent must exist
	 * @deprecated will become immutable, use {@link ScpPolicyBuilder}.
	 */
	@Deprecated(since = "3.2.0", forRemoval = true)
	public void setMkdirParentMustExist(boolean mkdirParentMustExist) {
		this.mkdirParentMustExist = mkdirParentMustExist;
	}
	
	/**
	 * Get whether the parent must exist for any directory creation operation to complete.
	 * 
	 * @return mkdir parent must exist
	 */
	public boolean isMkdirParentMustExist() {
		return mkdirParentMustExist;
	}
}
