/**
 * Author：zsq
 * Date：2024.1.7
 * 统计每个
 * Mapper：list of Doc of different Type -> <<Type, Word>, 1>
 * Reducer：list of <<Type, Word>, 1> -> <<Type, Word>, cnt>
 *      
 */

package com.zsq.NB;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

public class TestPreparation extends Configured implements Tool {

    public static class TestPreparation_Mapper extends Mapper<LongWritable, Text, Text, Text> {
        private Text mapKey = new Text();
        private Text mapValue = new Text();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            InputSplit inputsplit = context.getInputSplit();
            String className = ((FileSplit) inputsplit).getPath().getParent().getName();
            String fileName = ((FileSplit) inputsplit).getPath().getName();
            // simply use "a \t b" to combine key
            mapKey.set(className + "\t" + fileName);
            mapValue.set(value.toString());
            context.write(mapKey, mapValue);
        }
    }

    public static class TestPreparation_Reducer extends Reducer<Text, Text, Text, Text> {
        private Text result = new Text();
        private StringBuffer stringBuffer;

        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException {
            stringBuffer = new StringBuffer();
            for (Text value : values) {
                stringBuffer = stringBuffer.append(value.toString() + " ");
            }
            result.set(stringBuffer.toString());
            context.write(key, result);
        }
    }

    @Override
    public int run(String[] strings) throws Exception {
        Configuration conf = getConf();
        FileSystem hdfs = FileSystem.get(conf);

        Path preparationPath = new Path(Config.PREPARATION_PATH);
        if (hdfs.exists(preparationPath))
            hdfs.delete(preparationPath, true);

        Job job_Preparation = Job.getInstance(conf, "TestPreparation");
        job_Preparation.setJarByClass(TestPreparation.class);

        job_Preparation.setMapperClass(TestPreparation_Mapper.class);
        job_Preparation.setCombinerClass(TestPreparation_Reducer.class);
        job_Preparation.setReducerClass(TestPreparation_Reducer.class);
        // IO configure
        job_Preparation.setMapOutputKeyClass(Text.class);
        job_Preparation.setMapOutputValueClass(Text.class);
        job_Preparation.setOutputKeyClass(Text.class);
        job_Preparation.setOutputValueClass(Text.class);
        FileInputFormat.setInputDirRecursive(job_Preparation, true);
        FileInputFormat.addInputPath(job_Preparation, new Path(Config.TEST_SET_PATH));
        FileOutputFormat.setOutputPath(job_Preparation, preparationPath);
        return job_Preparation.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(),
                new TestPreparation(), args);
        System.exit(res);
    }
}
