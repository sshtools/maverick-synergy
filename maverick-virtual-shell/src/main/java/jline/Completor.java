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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */
package jline;

import java.util.List;

/**
 * A Completor is the mechanism by which tab-completion candidates will be
 * resolved.
 * 
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public interface Completor {
	/**
	 * Populates <i>candidates</i> with a list of possible completions for the
	 * <i>buffer</i>. The <i>candidates</i> list will not be sorted before being
	 * displayed to the user: thus, the complete method should sort the
	 * {@link List} before returning.
	 * 
	 * 
	 * @param buffer the buffer
	 * @param candidates the {@link List} of candidates to populate
	 * @return the index of the <i>buffer</i> for which the completion will be
	 *         relative
	 */
	int complete(String buffer, int cursor, List<String> candidates);
}
