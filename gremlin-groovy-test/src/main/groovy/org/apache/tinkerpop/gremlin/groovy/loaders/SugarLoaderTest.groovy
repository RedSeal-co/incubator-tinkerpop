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
package org.apache.tinkerpop.gremlin.groovy.loaders

import org.apache.tinkerpop.gremlin.AbstractGremlinTest
import org.apache.tinkerpop.gremlin.LoadGraphWith
import org.apache.tinkerpop.gremlin.groovy.util.SugarTestHelper
import org.apache.tinkerpop.gremlin.structure.*
import org.apache.tinkerpop.gremlin.structure.util.StringFactory
import org.junit.Test

import static org.junit.Assert.*
import static org.apache.tinkerpop.gremlin.process.traversal.P.*

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class SugarLoaderTest extends AbstractGremlinTest {

    @Override
    protected void afterLoadGraphWith(final Graph g) throws Exception {
        SugarTestHelper.clearRegistry(graphProvider)
    }

    @Test
    @LoadGraphWith(LoadGraphWith.GraphData.MODERN)
    public void shouldNotAllowSugar() {
        SugarTestHelper.clearRegistry(graphProvider)
        try {
            g.V
            fail("Without sugar loaded, the traversal should fail");
        } catch (MissingPropertyException e) {

        } catch (Exception e) {
            fail("Should fail with a MissingPropertyException: " + e)
        }

        try {
            g.V().out
            fail("Without sugar loaded, the traversal should fail");
        } catch (MissingPropertyException e) {

        } catch (Exception e) {
            fail("Should fail with a MissingPropertyException:" + e)
        }

        try {
            g.V().out().name
            fail("Without sugar loaded, the traversal should fail");
        } catch (MissingPropertyException e) {

        } catch (Exception e) {
            fail("Should fail with a MissingPropertyException: " + e)
        }
    }

    @Test
    @LoadGraphWith(LoadGraphWith.GraphData.MODERN)
    public void shouldAllowSugar() {
        SugarLoader.load()
        assertEquals(6, g.V.count.next())
        assertEquals(6, g.V.out.count.next())
        assertEquals(6, g.V.out.name.count.next())
        assertEquals(2, g.V(1).out.out.name.count.next());
        g.V(1).next().name = 'okram'
        assertEquals('okram', g.V(1).next().name);
        g.V(1).next()['name'] = 'marko a. rodriguez'
        assertEquals(["okram", "marko a. rodriguez"] as Set, g.V(1).values('name').toSet());
        assertEquals(29, g.V.age.is(eq(29)).next())
    }

    @Test
    @LoadGraphWith(LoadGraphWith.GraphData.MODERN)
    public void shouldUseTraverserCategoryCorrectly() {
        SugarLoader.load()
        g.V.as('a').out.as('x').name.as('b').back('x').has('age').map { [it.a, it.b, it.age] }.forEach {
            // println it;
            assertTrue(it[0] instanceof Vertex)
            assertTrue(it[1] instanceof String)
            assertTrue(it[2] instanceof Integer)
        };
    }

    @Test
    @LoadGraphWith(LoadGraphWith.GraphData.MODERN)
    public void shouldHaveProperToStringOfMixins() {
        SugarLoader.load();
        final Vertex vertex = graph.vertices().next();
        final Edge edge = graph.edges().next();
        final VertexProperty vertexProperty = vertex.property('name');
        final Property property = edge.property('weight');

        assertEquals(StringFactory.vertexString(vertex), vertex.toString());
        assertEquals(StringFactory.edgeString(edge), edge.toString());
        assertEquals(StringFactory.propertyString(vertexProperty), vertexProperty.toString());
        assertEquals(StringFactory.propertyString(property), property.toString());
        assertEquals(StringFactory.traversalSourceString(g), g.toString());
        //assertEquals(StringFactory.traversalSourceString(g.withPath()), g.withPath().toString());
        assertEquals(StringFactory.traversalString(g.V().out().asAdmin()), g.V().out().toString());
        assertEquals(StringFactory.traversalString(g.V.out), g.V.out.toString());
        assertEquals(convertToVertex(graph, "marko").toString(), g.V(convertToVertexId("marko")).next().toString())

    }
}
