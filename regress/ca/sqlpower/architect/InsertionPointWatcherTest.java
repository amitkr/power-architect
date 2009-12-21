/*
 * Copyright (c) 2008, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ca.sqlpower.architect;

import ca.sqlpower.sqlobject.StubSQLObject;
import junit.framework.TestCase;

public class InsertionPointWatcherTest extends TestCase {

    /**
     * SetUp creates this object and gives it three children.
     */
    private StubSQLObject so;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        so = new StubSQLObject();
        so.setName("Parent");
        
        StubSQLObject soChild;
        soChild = new StubSQLObject();
        soChild.setName("Child 0");
        so.addChild(soChild);
        soChild = new StubSQLObject();
        soChild.setName("Child 1");
        so.addChild(soChild);
        soChild = new StubSQLObject();
        soChild.setName("Child 2");
        so.addChild(soChild);
        
    }
    
    public void testRemoveBeforeInsertionPoint() throws Exception {
        InsertionPointWatcher<StubSQLObject> watcher = new InsertionPointWatcher<StubSQLObject>(so, 2, StubSQLObject.class);
        
        so.removeChild(so.getChild(1));
        assertEquals(1, watcher.getInsertionPoint());
    }

    public void testRemoveAtInsertionPoint() throws Exception {
        InsertionPointWatcher<StubSQLObject> watcher = new InsertionPointWatcher<StubSQLObject>(so, 2, StubSQLObject.class);
        
        so.removeChild(so.getChild(2));
        assertEquals(1, watcher.getInsertionPoint());
    }
    
    public void testRemoveAfterInsertionPoint() throws Exception {
        InsertionPointWatcher<StubSQLObject> watcher = new InsertionPointWatcher<StubSQLObject>(so, 1);
        
        so.removeChild(so.getChild(2));
        assertEquals(1, watcher.getInsertionPoint());
    }
    
    public void testDispose() {
        InsertionPointWatcher<StubSQLObject> watcher = new InsertionPointWatcher<StubSQLObject>(so, 1);

        assertEquals(1, so.getSPListeners().size());
        watcher.dispose();
        assertEquals(0, so.getSPListeners().size());
    }

}
