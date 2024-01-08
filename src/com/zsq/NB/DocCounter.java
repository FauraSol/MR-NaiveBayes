/**
 * Author：zsq
 * Date：2024.1.7
 * 统计每个类别的文件数目，用于计算先验概率
 * Mapper：list of Doc -> <type, 1>
 * Reducer：list of <type, 1> -> <type, cnt>
 */

package com.zsq.NB;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import java.io.IOException;

public class DocCounter extends Configured implements Tool {

    public static class DocCounterMapper extends Mapper<Text, BytesWritable, Text, IntWritable> {
        private final static IntWritable one = new IntWritable(1);

        @Override
        protected void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {
            context.write(key, one);
        }
    }

    public static class DocCounterReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        @Override
        protected void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    @Override
    public int run(String[] strings) throws Exception {
        Configuration conf = getConf();
        FileSystem hdfs = FileSystem.get(conf);
        // delete origin result
        Path docPath = new Path(Config.DOC_COUNT_PATH);
        if (hdfs.exists(docPath)) {
            hdfs.delete(docPath, true);
        }
        Job job_DocCounterJob = Job.getInstance(conf, "DocCounter");
        job_DocCounterJob.setJarByClass(DocCounter.class);
        // set mapper/reducer
        job_DocCounterJob.setMapperClass(DocCounterMapper.class);
        job_DocCounterJob.setCombinerClass(DocCounterReducer.class);
        job_DocCounterJob.setReducerClass(DocCounterReducer.class);
        // IO configure
        job_DocCounterJob.setInputFormatClass(MyFileInputFormat.class);
        job_DocCounterJob.setOutputKeyClass(Text.class);
        job_DocCounterJob.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job_DocCounterJob, new Path(Config.TRAIN_SET_PATH + Config.CLASS_A_NAME));
        FileInputFormat.addInputPath(job_DocCounterJob, new Path(Config.TRAIN_SET_PATH + Config.CLASS_B_NAME));
        FileOutputFormat.setOutputPath(job_DocCounterJob, docPath);
        return job_DocCounterJob.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(),
                new DocCounter(), args);
        System.exit(res);
    }
}
