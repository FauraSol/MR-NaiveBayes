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
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
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

public class WordCounter extends Configured implements Tool {
    public static class WordCounterMapper extends Mapper<LongWritable, Text, PairWriteable, IntWritable> {
        private PairWriteable keyOut = new PairWriteable();
        private final static IntWritable one = new IntWritable(1);

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            InputSplit inputSplit = context.getInputSplit();
            String className = ((FileSplit) inputSplit).getPath().getParent().getName();
            String word = value.toString();
            keyOut.set(className, word);
            context.write(keyOut, one);
        }
    }

    public static class WordCounterReducer extends Reducer<PairWriteable, IntWritable, PairWriteable, IntWritable> {
        private IntWritable result = new IntWritable();

        protected void reduce(PairWriteable key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException {
            int num = 0;
            for (IntWritable value : values) {
                num += value.get();
            }
            result.set(num);
            context.write(key, result);
        }
    }

    @Override
    public int run(String[] strings) throws Exception {
        Configuration conf = getConf();
        FileSystem hdfs = FileSystem.get(conf);
        Path wordPath = new Path(Config.WORD_COUNT_PATH);
        if (hdfs.exists(wordPath)) {
            hdfs.delete(wordPath, true);
        }
        Job job_WordCounterJob = Job.getInstance(conf, "WordCounter");
        job_WordCounterJob.setJarByClass(WordCounter.class);
        job_WordCounterJob.setMapperClass(WordCounterMapper.class);
        job_WordCounterJob.setReducerClass(WordCounterReducer.class);

        job_WordCounterJob.setMapOutputKeyClass(PairWriteable.class);
        job_WordCounterJob.setMapOutputValueClass(IntWritable.class);

        job_WordCounterJob.setOutputKeyClass(PairWriteable.class);
        job_WordCounterJob.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job_WordCounterJob, new Path(Config.TRAIN_SET_PATH + Config.CLASS_A_NAME));
        FileInputFormat.addInputPath(job_WordCounterJob, new Path(Config.TRAIN_SET_PATH + Config.CLASS_B_NAME));

        FileOutputFormat.setOutputPath(job_WordCounterJob, new Path(Config.WORD_COUNT_PATH));
        boolean result = job_WordCounterJob.waitForCompletion(true);
        return result ? 0 : -1;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(),
                new WordCounter(), args);
        System.exit(res);
    }
}
