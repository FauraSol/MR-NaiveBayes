package com.zsq.NB;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class MyFileInputFormat extends FileInputFormat<Text, BytesWritable> {

    @Override
    protected boolean isSplitable(JobContext context, Path fileName) {
        return false;
    }

    @Override
    public RecordReader<Text, BytesWritable> createRecordReader(InputSplit inputSplit,
            TaskAttemptContext taskAttemptContext)
            throws IOException, InterruptedException {
        MyFileRecordReader reader = new MyFileRecordReader();
        reader.initialize(inputSplit, taskAttemptContext);
        return reader;
    }

    public static class MyFileRecordReader extends RecordReader<Text, BytesWritable> {
        private FileSplit fileSplit;
        private Configuration conf;
        private Text key = new Text();
        private BytesWritable val = new BytesWritable();
        private boolean isFinished = false;

        @Override
        public void initialize(InputSplit inputSplit, TaskAttemptContext taskAttemptContext)
                throws IOException, InterruptedException {
            fileSplit = (FileSplit) inputSplit;
            isFinished = false;
            conf = taskAttemptContext.getConfiguration();
        }

        @Override
        public boolean nextKeyValue() throws IOException, InterruptedException {
            System.out.printf("path: %s\n", fileSplit.getPath().toString());
            if (!isFinished) {
                byte[] contents = new byte[(int) fileSplit.getLength()];
                FileSystem fs = null;
                FSDataInputStream fis = null;
                try {
                    Path path = fileSplit.getPath();
                    fs = path.getFileSystem(conf);
                    fis = fs.open(path);
                    IOUtils.readFully(fis, contents, 0, contents.length);
                    val.set(contents, 0, contents.length);
                    String classname = fileSplit.getPath().getParent().getName();
                    key.set(classname);
                } catch (Exception e) {
                    System.out.println(e);
                } finally {
                    IOUtils.closeStream(fis);
                }
                isFinished = true;
                return true;
            }
            return false;
        }

        @Override
        public Text getCurrentKey() throws IOException, InterruptedException {
            return key;
        }

        @Override
        public BytesWritable getCurrentValue() throws IOException, InterruptedException {
            return val;
        }

        @Override
        public float getProgress() throws IOException, InterruptedException {
            return isFinished ? 1.0f : 0.0f;
        }

        @Override
        public void close() throws IOException {

        }
    }
}
