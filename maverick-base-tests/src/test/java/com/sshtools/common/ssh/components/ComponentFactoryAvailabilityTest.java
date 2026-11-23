package com.sshtools.common.ssh.components;

/*-
 * #%L
 * Base API Tests
 * %%
 * Copyright (C) 2002 - 2026 JADAPTIVE Limited
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ComponentFactoryAvailabilityTest {

    @Test
    public void removeDisablesButRetainsImplementationForReuse() throws Exception {
        ComponentFactory<TestComponent> factory = new ComponentFactory<>(null);
        factory.add(new TestFactory());

        assertTrue(factory.contains("test-component"));
        factory.remove("test-component");
        assertFalse(factory.contains("test-component"));
        assertFalse(factory.hasComponents());
        assertEquals("", factory.filter("test-component,missing"));
        assertNotNull(factory.getInstance("test-component"));

        factory.add(new TestFactory());
        assertTrue(factory.contains("test-component"));
    }

    private static class TestComponent implements Component {
    }

    private static class TestFactory implements ComponentInstanceFactory<TestComponent> {
        @Override
        public TestComponent create() {
            return new TestComponent();
        }

        @Override
        public String[] getKeys() {
            return new String[] { "test-component" };
        }
    }
}
