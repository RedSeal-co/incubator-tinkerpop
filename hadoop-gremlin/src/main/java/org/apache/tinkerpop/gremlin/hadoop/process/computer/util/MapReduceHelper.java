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
package org.apache.tinkerpop.gremlin.hadoop.process.computer.util;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.process.computer.HadoopCombine;
import org.apache.tinkerpop.gremlin.hadoop.process.computer.HadoopMap;
import org.apache.tinkerpop.gremlin.hadoop.process.computer.HadoopReduce;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.InputOutputHelper;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.ObjectWritable;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.ObjectWritableComparator;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.ObjectWritableIterator;
import org.apache.tinkerpop.gremlin.hadoop.structure.util.ConfUtil;
import org.apache.tinkerpop.gremlin.process.computer.MapReduce;
import org.apache.tinkerpop.gremlin.process.computer.Memory;
import org.apache.tinkerpop.gremlin.process.computer.VertexProgram;

import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class MapReduceHelper {

    private MapReduceHelper() {
    }

    public static void executeMapReduceJob(final MapReduce mapReduce, final Memory.Admin memory, final Configuration configuration) throws IOException, ClassNotFoundException, InterruptedException {
        final boolean vertexProgramExists = configuration.get(VertexProgram.VERTEX_PROGRAM, null) != null;
        final Configuration newConfiguration = new Configuration(configuration);
        final BaseConfiguration apacheConfiguration = new BaseConfiguration();
        apacheConfiguration.setDelimiterParsingDisabled(true);
        mapReduce.storeState(apacheConfiguration);
        ConfUtil.mergeApacheIntoHadoopConfiguration(apacheConfiguration, newConfiguration);
        if (!mapReduce.doStage(MapReduce.Stage.MAP)) {
            final Path memoryPath = new Path(configuration.get(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION) + "/" + mapReduce.getMemoryKey());
            mapReduce.addResultToMemory(memory, new ObjectWritableIterator(configuration, memoryPath));
        } else {
            final Optional<Comparator<?>> mapSort = mapReduce.getMapKeySort();
            final Optional<Comparator<?>> reduceSort = mapReduce.getReduceKeySort();

            newConfiguration.setClass(Constants.GREMLIN_HADOOP_MAP_REDUCE_CLASS, mapReduce.getClass(), MapReduce.class);
            final Job job = new Job(newConfiguration, mapReduce.toString());
            HadoopGraph.LOGGER.info(Constants.GREMLIN_HADOOP_JOB_PREFIX + mapReduce.toString());
            job.setJarByClass(HadoopGraph.class);
            if (mapSort.isPresent())
                job.setSortComparatorClass(ObjectWritableComparator.ObjectWritableMapComparator.class);
            job.setMapperClass(HadoopMap.class);
            if (mapReduce.doStage(MapReduce.Stage.REDUCE)) {
                if (mapReduce.doStage(MapReduce.Stage.COMBINE))
                    job.setCombinerClass(HadoopCombine.class);
                job.setReducerClass(HadoopReduce.class);
            } else {
                if (mapSort.isPresent()) {
                    job.setReducerClass(Reducer.class);
                } else {
                    job.setNumReduceTasks(0);
                }
            }
            job.setMapOutputKeyClass(ObjectWritable.class);
            job.setMapOutputValueClass(ObjectWritable.class);
            job.setOutputKeyClass(ObjectWritable.class);
            job.setOutputValueClass(ObjectWritable.class);
            job.setInputFormatClass(vertexProgramExists ?
                    InputOutputHelper.getInputFormat((Class) newConfiguration.getClass(Constants.GREMLIN_HADOOP_GRAPH_OUTPUT_FORMAT, OutputFormat.class)) :
                    (Class) newConfiguration.getClass(Constants.GREMLIN_HADOOP_GRAPH_INPUT_FORMAT, InputFormat.class));
            job.setOutputFormatClass(SequenceFileOutputFormat.class);
            // if there is no vertex program, then grab the graph from the input location
            final Path graphPath = vertexProgramExists ?
                    new Path(newConfiguration.get(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION) + "/" + Constants.HIDDEN_G) :
                    new Path(newConfiguration.get(Constants.GREMLIN_HADOOP_INPUT_LOCATION));
            Path memoryPath = new Path(newConfiguration.get(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION) + "/" + (reduceSort.isPresent() ? mapReduce.getMemoryKey() + "-temp" : mapReduce.getMemoryKey()));
            if (FileSystem.get(newConfiguration).exists(memoryPath)) {
                FileSystem.get(newConfiguration).delete(memoryPath, true);
            }
            FileInputFormat.setInputPaths(job, graphPath);
            FileOutputFormat.setOutputPath(job, memoryPath);
            job.waitForCompletion(true);

            // if there is a reduce sort, we need to run another identity MapReduce job
            if (reduceSort.isPresent()) {
                final Job reduceSortJob = new Job(newConfiguration, "ReduceKeySort");
                reduceSortJob.setSortComparatorClass(ObjectWritableComparator.ObjectWritableReduceComparator.class);
                reduceSortJob.setMapperClass(Mapper.class);
                reduceSortJob.setReducerClass(Reducer.class);
                reduceSortJob.setMapOutputKeyClass(ObjectWritable.class);
                reduceSortJob.setMapOutputValueClass(ObjectWritable.class);
                reduceSortJob.setOutputKeyClass(ObjectWritable.class);
                reduceSortJob.setOutputValueClass(ObjectWritable.class);
                reduceSortJob.setInputFormatClass(SequenceFileInputFormat.class);
                reduceSortJob.setOutputFormatClass(SequenceFileOutputFormat.class);
                FileInputFormat.setInputPaths(reduceSortJob, memoryPath);
                final Path sortedMemoryPath = new Path(newConfiguration.get(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION) + "/" + mapReduce.getMemoryKey());
                FileOutputFormat.setOutputPath(reduceSortJob, sortedMemoryPath);
                reduceSortJob.waitForCompletion(true);
                FileSystem.get(newConfiguration).delete(memoryPath, true); // delete the temporary memory path
                memoryPath = sortedMemoryPath;
            }
            mapReduce.addResultToMemory(memory, new ObjectWritableIterator(configuration, memoryPath));
        }
    }
}
