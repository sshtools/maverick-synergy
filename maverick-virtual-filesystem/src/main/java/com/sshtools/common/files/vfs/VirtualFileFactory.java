package com.sshtools.common.files.vfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import com.sshtools.common.events.Event;
import com.sshtools.common.events.EventCodes;
import com.sshtools.common.files.AbstractFileFactory;
import com.sshtools.common.files.AbstractFileHomeFactory;
import com.sshtools.common.files.FileSystemUtils;
import com.sshtools.common.logger.Log;
import com.sshtools.common.permissions.PermissionDeniedException;
import com.sshtools.common.ssh.SshConnection;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

public class VirtualFileFactory implements AbstractFileFactory<VirtualFile> {

	private static final String MOUNT_MANAGER = "mountManager";

	private static final String CACHE = "vfsCache";

	protected List<VirtualMountTemplate> mountTemplates = new ArrayList<VirtualMountTemplate>();
	protected VirtualMountTemplate homeMountTemplate;
	protected boolean cached = true;

	public VirtualFileFactory(AbstractFileFactory<?> defaultFileFactory) {
		homeMountTemplate = new VirtualMountTemplate("/",
				"virtualfs/home/${username}", defaultFileFactory);
	}

	public VirtualFileFactory(VirtualMountTemplate homeMount,
			VirtualMountTemplate... additionalMounts) {
		this.homeMountTemplate = homeMount;
		if(Log.isDebugEnabled()) {
			Log.debug("Virtual file factory created with home mount "
					+ homeMount.getMount() + " to path " + homeMount.getRoot());
		}
		for (VirtualMountTemplate t : additionalMounts) {
			mountTemplates.add(t);
			if(Log.isDebugEnabled()) {
				Log.debug("Virtual file factory created with additional mount "
						+ t.getMount() + " to path " + t.getRoot());
			}
		}
	}

	public boolean isCached() {
		return cached;
	}

	public void setCached(boolean cached) {
		this.cached = cached;
	}

	private Cache getCache(SshConnection con) {
		if (cached) {
			return null;
		}
		if (!con.containsProperty(CACHE)) {
			CacheManager cacheManager = CacheManager.getInstance();
			int oneHour = 60 * 60;
			cacheManager.addCache(new Cache(con.getSessionId(), 16384, false, false,
					oneHour, oneHour));
			con.setProperty(CACHE, cacheManager);
		}
		return ((CacheManager) con.getProperty(CACHE))
				.getCache(con.getSessionId());
	}

	private String canonicalisePath(String path) {
		StringTokenizer t = new StringTokenizer(path, "/", true);
		Stack<String> pathStack = new Stack<String>();
		while (t.hasMoreTokens()) {
			String e = t.nextToken();
			if (e.equals("..")) {
				if (pathStack.size() > 1) {
					pathStack.pop();
					pathStack.pop();
				}

			} else {
				if (pathStack.size() > 0 && pathStack.peek() == "/"
						&& e.equals("/")) {
					continue;
				}
				pathStack.push(e);
			}
		}
		String ret = "";
		for (String e : pathStack) {
			ret += e;
		}

		if (!ret.startsWith("/")) {
			ret = FileSystemUtils
					.addTrailingSlash(homeMountTemplate.getMount()) + ret;
		}
		return ret;

	}

	public VirtualFile getFile(String path, SshConnection con)
			throws PermissionDeniedException, IOException {

		VirtualMountManager mgr = getMountManager(con);

		String virtualPath;

		if (path.equals("")) {
			virtualPath = mgr.getDefaultMount().getMount();
			
		} else {
			virtualPath = canonicalisePath(path);
		}

		VirtualMount[] mounts = mgr.getMounts(virtualPath);
		if (!virtualPath.equals("") && mounts.length > 0) {
			String mountPath = FileSystemUtils.addTrailingSlash(virtualPath);

			if (!mountPath.equals("/")) {
				for (VirtualMount m : mounts) {
					String thisMountPath = FileSystemUtils.addTrailingSlash(m
							.getMount());
					if (thisMountPath.startsWith(mountPath)
							&& !thisMountPath.equals(mountPath)) {
						return new VirtualMountFile(
								FileSystemUtils
										.removeTrailingSlash(virtualPath),
								mgr.getMount(virtualPath), mgr, con);
					}
				}
			} else {
				VirtualMount rootMount = mgr.getMount("/");
				if (!rootMount.isFilesystemRoot()
						|| (rootMount.isFilesystemRoot() && !rootMount
								.isDefault())) {
					return new VirtualMountFile(virtualPath, rootMount, mgr,
							con);
				}
			}
			// If we reached here we are file system root and default so we
			// don't use
			// the virtual mount file but instead list actual files and inject
			// any mounts
			// below us
		}

		if (!virtualPath.equals("/")) {
			virtualPath = FileSystemUtils.removeTrailingSlash(virtualPath);
		}

		VirtualMount m = getMountManager(con).getMount(virtualPath);
		Cache cache = getCache(con);
		if(m.isCached()) {
			if(cache != null) {
				Element e = cache.get(virtualPath);
				if (e != null) {
					System.out.println("**** " + virtualPath + " (" + path + ") comes from cache");
					return (VirtualFile) e.getObjectValue();
				}
			}
		}
		VirtualFile f = new VirtualMappedFile(virtualPath, con, m, this);
		if (m.isCached()) {
			cache.put(new Element(virtualPath, f));
		}
		return f;

	}

	public VirtualMountTemplate getHomeMountTemplate() {
		return homeMountTemplate;
	}

	public VirtualMountManager getMountManager(SshConnection con)
			throws IOException, PermissionDeniedException {

		if (!con.containsProperty(MOUNT_MANAGER)) {
			con.setProperty(
							MOUNT_MANAGER,
							new VirtualMountManager(
									con,
									this,
									homeMountTemplate,
									mountTemplates
											.toArray(new VirtualMountTemplate[0])));
		}
		return (VirtualMountManager) con.getProperty(
				MOUNT_MANAGER);
	}

	public AbstractFileFactory<?> getDefaultFileFactory() {
		return homeMountTemplate.getActualFileFactory();
	}

	public void addMountTemplate(VirtualMountTemplate virtualMount) {
		mountTemplates.add(virtualMount);
	}

	public void init(String defaultPath) throws PermissionDeniedException,
			IOException {
		throw new IllegalAccessError(
				"VirtualFileFactory is not a physical file system");
	}

	public Event populateEvent(Event evt) {
		try {
			return evt
					.addAttribute(
							EventCodes.ATTRIBUTE_MOUNT_MANAGER,
							getMountManager((SshConnection) evt
									.getAttribute(EventCodes.ATTRIBUTE_CONNECTION)));
		} catch (Exception e) {
			return evt;
		}
	}

	public VirtualFile getDefaultPath(SshConnection con)
			throws PermissionDeniedException, IOException {
		return getFile("", con);
	}

	public void init(AbstractFileHomeFactory homeFactory) {

	}

}
