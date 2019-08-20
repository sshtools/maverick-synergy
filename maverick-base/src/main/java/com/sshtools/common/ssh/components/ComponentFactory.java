/**
 * (c) 2002-2019 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Maverick Synergy Java SSH API.
 *
 * Maverick Synergy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Maverick Synergy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Maverick Synergy.  If not, see <https://www.gnu.org/licenses/>.
 */
/* HEADER */
package com.sshtools.common.ssh.components;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import com.sshtools.common.ssh.SshException;

/**
 * <p>
 * A utility class used to store the available transport components and provide
 * delimited listing as required in the key exchange initialization process.
 * </p>
 * 
 * @author Lee David Painter
 */
public class ComponentFactory<T> implements Cloneable {

	/**
	 * The supported components stored in a Hashtable with a String key as the
	 * component name such as "3des-cbc" and a Class value storing the
	 * implementation class.
	 */
	protected Hashtable<String, Class<? extends T>> supported = new Hashtable<String, Class<? extends T>>();
	protected Vector<String> order = new Vector<String>();

	private boolean locked = false;
	private ComponentManager componentManager;
	
	public ComponentFactory(ComponentManager componentManager) {
		this.componentManager = componentManager;
	}
	
	public synchronized String changePositionofAlgorithm(String name,
			int position) throws SshException {
		if (position < 0) {
			throw new SshException("index out of bounds",
					SshException.BAD_API_USAGE);
		}
		
		if (!contains(name)) {
			throw new SshException(String.format("%s is not a supported algorithm", name),
					SshException.BAD_API_USAGE);
		}

		if (position >= order.size()) {
			position = order.size();
		}

		int currentLocation = order.indexOf(name);
		if (currentLocation < position) {
			order.insertElementAt(name, position);
			if(currentLocation > 0) {
			  order.removeElementAt(currentLocation);
			}
		} else {
			order.removeElementAt(currentLocation);
			order.insertElementAt(name, position);
		}

		return (String) order.elementAt(0);
	}
	
	public Collection<String> names() {
		return supported.keySet();
	}

	public synchronized String order(String[] ordering) throws SshException {
		
		Vector<String> newOrder = new Vector<String>();
		
		for(String alg : ordering) {
			if(supported.containsKey(alg)) {
				newOrder.add(alg);
			}
		}
		
		if(newOrder.size() == 0) {
			throw new SshException("No algorithms supported",
					SshException.BAD_API_USAGE);
		}
		
		order = newOrder;
		
		return order.get(0);
		
	}
	public synchronized String createNewOrdering(int[] ordering)
			throws SshException {
		if (ordering.length > order.size()) {
			throw new SshException("too many indicies",
					SshException.BAD_API_USAGE);
		}

		// move indices specified in ordering to end of vector
		for (int i = 0; i < ordering.length; i++) {
			if (!(ordering[i] >= 0 && ordering[i] < order.size())) {
				throw new SshException("index out of bounds",
						SshException.BAD_API_USAGE);
			}
			order.insertElementAt(order.elementAt(ordering[i]), order.size());
		}
		// sort ordering indices so that remove lowest indices first
		Arrays.sort(ordering);
		// remove from order starting from end
		for (int i = (ordering.length - 1); i >= 0; i--) {
			order.removeElementAt(ordering[i]);
		}
		// move ones moved to end to beginning starting from end
		for (int i = 0; i < ordering.length; i++) {
			String element = order.elementAt(order.size() - 1);
			order.removeElementAt(order.size() - 1);
			order.insertElementAt(element, 0);
		}

		return order.elementAt(0);
	}

	/**
	 * Determine whether the factory supports a given component type.
	 * 
	 * @param name
	 * @return <code>true</code> if the component is supported otherwise
	 *         <code>false</code>
	 */
	public boolean contains(String name) {
		return supported.containsKey(name);
	}

	/**
	 * List the types of components supported by this factory. Returns the list
	 * as a comma delimited string with the preferred value as the first entry
	 * in the list. If the preferred value is "" then the list is returned
	 * unordered.
	 * 
	 * @param preferred
	 *            The preferred component type.
	 * @param ignores Any items you want to exclude from the returned list.
	 * @return A comma delimited String of component types; for example
	 *         "3des-cbc,blowfish-cbc"
	 */
	public synchronized String list(String preferred, String... ignores) {
		return createDelimitedList(preferred, ignores);
	}

	/**
	 * List the types of components supported by this factory. Returns the list
	 * as a comma delimited string with the preferred value as the first entry
	 * in the list. If the preferred value is "" then the list is returned
	 * unordered.
	 * 
	 * @param preferred
	 *            The preferred component type.
	 * @return A comma delimited String of component types; for example
	 *         "3des-cbc,blowfish-cbc"
	 */
	public synchronized String list(String preferred) {
		return createDelimitedList(preferred);
	}
	
	/**
	 * Add a new component type to the factory. This method throws an exception
	 * if the class cannot be resolved. The name of the component IS NOT
	 * verified to allow component implementations to be overridden.
	 * 
	 * @param name
	 * @param cls
	 * @throws ClassNotFoundException
	 */
	public synchronized void add(String name, Class<? extends T> cls) {

		if (locked) {
			throw new IllegalStateException(
					"Component factory is locked. Components cannot be added");
		}

		supported.put(name, cls);
		// add name to end of order vector
		if (!order.contains(name))
			order.addElement(name);
	}

	/**
	 * Get a new instance of a supported component.
	 * 
	 * @param name
	 *            The name of the component; for example "3des-cbc"
	 * @return the newly instantiated object
	 * @throws ClassNotFoundException
	 */
	public T getInstance(String name) throws SshException {
		if (supported.containsKey(name)) {
			try {
				return createInstance(name, supported.get(name));
			} catch (Throwable t) {
				throw new SshException(t.getMessage(),
						SshException.INTERNAL_ERROR, t);
			}
		}
		throw new SshException(name + " is not supported",
				SshException.UNSUPPORTED_ALGORITHM);
	}

	/**
	 * Override this method to create an instance of the component.
	 * 
	 * @param cls
	 * @return the newly instantiated object
	 * @throws java.lang.Throwable
	 */
	protected T createInstance(String name, Class<? extends T> cls)
			throws Throwable {
		return cls.newInstance();
	}

	/**
	 * Create a delimited list of supported components.
	 * 
	 * @param preferred
	 * @return a comma delimited list
	 */
	private synchronized String createDelimitedList(String preferred, String... ignores) {
		StringBuffer listBuf = new StringBuffer();
		int prefIndex = order.indexOf(preferred);
		// remove preferred and add it back at the end to ensure it is not
		// duplicated in the list returned
		if (prefIndex != -1 && !isDisabled(preferred)) {
			listBuf.append(preferred);
		}

		for (int i = 0; i < order.size(); i++) {
			if (prefIndex == i || isDisabled(order.elementAt(i))) {
				continue;
			}
			boolean ignoreItem = false;
			for(String ignore : ignores) {
				if(order.elementAt(i).equals(ignore)) {
					ignoreItem = true;
					break;
				}
			}
			if(!ignoreItem) {
				if(listBuf.length() > 0) {
					listBuf.append(",");
				}
				listBuf.append((String) order.elementAt(i));
			}
		}

		return listBuf.toString();
	}

	private boolean isDisabled(String alg) {
		return componentManager!=null && componentManager.isDisabled(alg);
	}
	
	/**
	 * Remove a supported component
	 * 
	 * @param name
	 */
	public synchronized void remove(String name) {

		// remove name from order vector
		order.removeElement(name);
	}

	/**
	 * Clear all of the entries in this component factory.
	 */
	public synchronized void clear() {

		if (locked) {
			throw new IllegalStateException(
					"Component factory is locked. Removing all components renders it unusable");
		}

		supported.clear();
		// clear order vector
		order.removeAllElements();
	}

	@SuppressWarnings("unchecked")
	public Object clone() {
		ComponentFactory<T> clone = new ComponentFactory<T>(componentManager);
		clone.order = (Vector<String>) order.clone();
		clone.supported = (Hashtable<String, Class<? extends T>>) supported
				.clone();
		return clone;
	}

	public String[] toArray() {
		return (String[]) order.toArray(new String[order.size()]);
	}

	public synchronized void removeAllBut(String names) {
		StringTokenizer t = new StringTokenizer(names, ",");
		Vector<String> v = new Vector<String>();
		while (t.hasMoreTokens()) {
			String name = t.nextToken();
			if (supported.containsKey(name))
				v.add(name);
		}

		for (Enumeration<String> e = supported.keys(); e.hasMoreElements();) {
			String name = (String) e.nextElement();
			if (!v.contains(name)) {
				remove(name);
			}
		}
	}

	public void lockComponents() {
		this.locked = true;
	}
}
