/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

import android.os.WorkSource.WorkChain;

import junit.framework.TestCase;

import java.util.List;

/**
 * Provides unit tests for hidden / unstable WorkSource APIs that are not CTS testable.
 *
 * These tests will be moved to CTS when finalized.
 */
public class WorkSourceTest extends TestCase {
    public void testWorkChain_add() {
        WorkChain wc1 = new WorkChain();
        wc1.addNode(56, null);

        assertEquals(56, wc1.getUids()[0]);
        assertEquals(null, wc1.getTags()[0]);
        assertEquals(1, wc1.getSize());

        wc1.addNode(57, "foo");
        assertEquals(56, wc1.getUids()[0]);
        assertEquals(null, wc1.getTags()[0]);
        assertEquals(57, wc1.getUids()[1]);
        assertEquals("foo", wc1.getTags()[1]);

        assertEquals(2, wc1.getSize());
    }

    public void testWorkChain_equalsHashCode() {
        WorkChain wc1 = new WorkChain();
        WorkChain wc2 = new WorkChain();

        assertEquals(wc1, wc2);
        assertEquals(wc1.hashCode(), wc2.hashCode());

        wc1.addNode(1, null);
        wc2.addNode(1, null);
        assertEquals(wc1, wc2);
        assertEquals(wc1.hashCode(), wc2.hashCode());

        wc1.addNode(2, "tag");
        wc2.addNode(2, "tag");
        assertEquals(wc1, wc2);
        assertEquals(wc1.hashCode(), wc2.hashCode());

        wc1 = new WorkChain();
        wc2 = new WorkChain();
        wc1.addNode(5, null);
        wc2.addNode(6, null);
        assertFalse(wc1.equals(wc2));
        assertFalse(wc1.hashCode() == wc2.hashCode());

        wc1 = new WorkChain();
        wc2 = new WorkChain();
        wc1.addNode(5, "tag1");
        wc2.addNode(5, "tag2");
        assertFalse(wc1.equals(wc2));
        assertFalse(wc1.hashCode() == wc2.hashCode());
    }

    public void testWorkChain_constructor() {
        WorkChain wc1 = new WorkChain();
        wc1.addNode(1, "foo")
            .addNode(2, null)
            .addNode(3, "baz");

        WorkChain wc2 = new WorkChain(wc1);
        assertEquals(wc1, wc2);

        wc1.addNode(4, "baz");
        assertFalse(wc1.equals(wc2));
    }

    public void testDiff_workChains() {
        WorkSource ws1 = new WorkSource();
        ws1.add(50);
        ws1.createWorkChain().addNode(52, "foo");
        WorkSource ws2 = new WorkSource();
        ws2.add(50);
        ws2.createWorkChain().addNode(60, "bar");

        // Diffs don't take WorkChains into account for the sake of backward compatibility.
        assertFalse(ws1.diff(ws2));
        assertFalse(ws2.diff(ws1));
    }

    public void testEquals_workChains() {
        WorkSource ws1 = new WorkSource();
        ws1.add(50);
        ws1.createWorkChain().addNode(52, "foo");

        WorkSource ws2 = new WorkSource();
        ws2.add(50);
        ws2.createWorkChain().addNode(52, "foo");

        assertEquals(ws1, ws2);

        // Unequal number of WorkChains.
        ws2.createWorkChain().addNode(53, "baz");
        assertFalse(ws1.equals(ws2));

        // Different WorkChain contents.
        WorkSource ws3 = new WorkSource();
        ws3.add(50);
        ws3.createWorkChain().addNode(60, "bar");

        assertFalse(ws1.equals(ws3));
        assertFalse(ws3.equals(ws1));
    }

    public void testWorkSourceParcelling() {
        WorkSource ws = new WorkSource();

        WorkChain wc = ws.createWorkChain();
        wc.addNode(56, "foo");
        wc.addNode(75, "baz");
        WorkChain wc2 = ws.createWorkChain();
        wc2.addNode(20, "foo2");
        wc2.addNode(30, "baz2");

        Parcel p = Parcel.obtain();
        ws.writeToParcel(p, 0);
        p.setDataPosition(0);

        WorkSource unparcelled = WorkSource.CREATOR.createFromParcel(p);

        assertEquals(unparcelled, ws);
    }

    public void testSet_workChains() {
        WorkSource ws1 = new WorkSource();
        ws1.add(50);

        WorkSource ws2 = new WorkSource();
        ws2.add(60);
        WorkChain wc = ws2.createWorkChain();
        wc.addNode(75, "tag");

        ws1.set(ws2);

        // Assert that the WorkChains are copied across correctly to the new WorkSource object.
        List<WorkChain> workChains = ws1.getWorkChains();
        assertEquals(1, workChains.size());

        assertEquals(1, workChains.get(0).getSize());
        assertEquals(75, workChains.get(0).getUids()[0]);
        assertEquals("tag", workChains.get(0).getTags()[0]);

        // Also assert that a deep copy of workchains is made, so the addition of a new WorkChain
        // or the modification of an existing WorkChain has no effect.
        ws2.createWorkChain();
        assertEquals(1, ws1.getWorkChains().size());

        wc.addNode(50, "tag2");
        assertEquals(1, ws1.getWorkChains().size());
        assertEquals(1, ws1.getWorkChains().get(0).getSize());
    }

    public void testSet_nullWorkChain() {
        WorkSource ws = new WorkSource();
        ws.add(60);
        WorkChain wc = ws.createWorkChain();
        wc.addNode(75, "tag");

        ws.set(null);
        assertEquals(0, ws.getWorkChains().size());
    }

    public void testAdd_workChains() {
        WorkSource ws = new WorkSource();
        ws.createWorkChain().addNode(70, "foo");

        WorkSource ws2 = new WorkSource();
        ws2.createWorkChain().addNode(60, "tag");

        ws.add(ws2);

        // Check that the new WorkChain is added to the end of the list.
        List<WorkChain> workChains = ws.getWorkChains();
        assertEquals(2, workChains.size());
        assertEquals(1, workChains.get(1).getSize());
        assertEquals(60, ws.getWorkChains().get(1).getUids()[0]);
        assertEquals("tag", ws.getWorkChains().get(1).getTags()[0]);

        // Adding the same WorkChain twice should be a no-op.
        ws.add(ws2);
        assertEquals(2, workChains.size());
    }

    public void testSet_noWorkChains() {
        WorkSource ws = new WorkSource();
        ws.set(10);
        assertEquals(1, ws.size());
        assertEquals(10, ws.get(0));

        WorkSource ws2 = new WorkSource();
        ws2.set(20, "foo");
        assertEquals(1, ws2.size());
        assertEquals(20, ws2.get(0));
        assertEquals("foo", ws2.getName(0));
    }
}
