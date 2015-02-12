/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.tinkergraph.structure

import org.apache.tinkerpop.gremlin.groovy.loaders.SugarLoader
import org.apache.tinkerpop.gremlin.process.graph.traversal.GraphTraversal
import org.apache.tinkerpop.gremlin.process.graph.traversal.__
import org.apache.tinkerpop.gremlin.structure.Graph
import org.junit.Ignore
import org.junit.Test

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class GroovyTinkerGraphTest {

    static {
        SugarLoader.load();
    }

    @Test
    @Ignore
    public void testPlay3() throws Exception {
        Graph g = TinkerFactory.createModern();
        GraphTraversal t = SELECT 'a', 'b' FROM g.V.as('a').has(__.name & __.age.gt(29) | __.inE.count.gt(1l) & __.lang).'name'.as('b');

        System.out.println(t.toString());
        t.forEachRemaining { System.out.println(it) };
        System.out.println(t.toString());
    }
}