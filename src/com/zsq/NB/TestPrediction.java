package com.zsq.NB;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class TestPrediction extends Configured implements Tool {
    private static HashMap<String, Double> priorProbability = new HashMap<String, Double>();
    private static HashMap<String, Double> conditionalProbability = new HashMap<>();
    // 计算类的先验概率

    public static void Get_PriorProbability() throws IOException {
        Configuration conf = new Configuration();
        FSDataInputStream fsInputStream = null;
        BufferedReader bufferedReader = null;
        String lineValue = null;
        double docNum = 0;
        try {
            FileSystem hdfs = FileSystem.get(URI.create(Config.DOC_COUNT_PATH +
                    "part-r-00000"), conf);
            fsInputStream = hdfs.open(new Path(Config.DOC_COUNT_PATH + "/part-r-00000"));
            bufferedReader = new BufferedReader(new InputStreamReader(fsInputStream));
            while ((lineValue = bufferedReader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(lineValue);
                String className = tokenizer.nextToken();
                String num_C_Tmp = tokenizer.nextToken();
                double numC = Double.parseDouble(num_C_Tmp);
                priorProbability.put(className, numC);
                docNum = docNum + numC;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bufferedReader.close();
        }

        Iterator<Map.Entry<String, Double>> it = priorProbability.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Double> entry = (Map.Entry<String, Double>) it.next();
            double value = Double.parseDouble(entry.getValue().toString());
            value /= docNum;
            entry.setValue(value);
        }
    }

    public static void Get_ConditionProbability() throws IOException {
        String filePath = Config.WORD_COUNT_PATH + "/part-r-00000";
        Configuration conf = new Configuration();
        FSDataInputStream fsInputStream = null;
        BufferedReader bufferedReader = null;
        String lineValue = null;
        HashMap<String, Double> wordSum = new HashMap<String, Double>(); // 存放的为<类名，单词总数>

        try {
            FileSystem hdfs = FileSystem.get(URI.create(filePath), conf);
            fsInputStream = hdfs.open(new Path(filePath));
            bufferedReader = new BufferedReader(new InputStreamReader(fsInputStream));
            while ((lineValue = bufferedReader.readLine()) != null) { // 按行读取
                StringTokenizer tokenizer = new StringTokenizer(lineValue);
                String className = tokenizer.nextToken();
                // 想按go的思路用_表示无用变量，结果java好像把_保留了，这样命名从C-like又很奇怪
                String _word = tokenizer.nextToken();
                double numWord = Double.parseDouble(tokenizer.nextToken());
                if (wordSum.containsKey(className))
                    wordSum.put(className, wordSum.get(className) + numWord + 1.0);// 加1.0是因为每一次都是一个不重复的单词
                else
                    wordSum.put(className, numWord + 1.0);
            }
            fsInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // 计算条件概率
        try {
            FileSystem hdfs = FileSystem.get(URI.create(filePath), conf);
            fsInputStream = hdfs.open(new Path(filePath));
            bufferedReader = new BufferedReader(new InputStreamReader(fsInputStream));
            while ((lineValue = bufferedReader.readLine()) != null) { // 按行读取
                StringTokenizer tokenizer = new StringTokenizer(lineValue);
                String className = tokenizer.nextToken();
                String word = tokenizer.nextToken();
                double numWord = Double.parseDouble(tokenizer.nextToken());
                String key = className + "\t" + word;
                conditionalProbability.put(key, (numWord + 1.0) / wordSum.get(className));
            }
            fsInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        // 对测试集中出现的新单词定义概率
        Iterator<Map.Entry<String, Double>> iterator = wordSum.entrySet().iterator();
        // 获取key和value的set
        while (iterator.hasNext()) {
            Map.Entry<String, Double> entry = (Map.Entry<String, Double>) iterator.next(); // 把hashmap转成Iterator再迭代到entry
            Object key = entry.getKey(); // 从entry获取key
            conditionalProbability.put(key.toString(), 1.0 /
                    Double.parseDouble(entry.getValue().toString()));
        }
    }

    public static class TestPrediction_Mapper extends Mapper<LongWritable, Text, PairWriteable, PairWriteable> {
        public void setup(Context context) throws IOException {
            Get_PriorProbability(); // 先验概率
            Get_ConditionProbability(); // 条件概率
        }

        private PairWriteable mapKey = new PairWriteable();
        private PairWriteable mapValue = new PairWriteable();

        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String[] lineValues = value.toString().split("\\s"); // 分词，按照空白字符切割
            String class_Name = lineValues[0]; // 得到类名
            String fileName = lineValues[1]; // 得到文件名
            for (Map.Entry<String, Double> entry : priorProbability.entrySet()) {
                String className = entry.getKey();
                mapKey.set(class_Name, fileName);// 新的键值的key为<类明 文档名>
                double tempValue = Math.log(entry.getValue());// 构建临时键值对的value为各概率相乘,转化为各概率取对数再相加
                for (int i = 2; i < lineValues.length; i++) {
                    String tempKey = className + "\t" + lineValues[i];// 构建临时键值对<class_word>,在wordsProbably表中查找对应的概率
                    if (conditionalProbability.containsKey(tempKey)) {
                        // 如果测试文档的单词在训练集中出现过，则直接加上之前计算的概率
                        tempValue += Math.log(conditionalProbability.get(tempKey));
                    } else {// 如果测试文档中出现了新单词则加上之前计算新单词概率
                        tempValue += Math.log(conditionalProbability.get(className));
                    }
                }
                mapValue.set(className, Double.toString(tempValue));// 新的键值的value为<类名 概率>
                context.write(mapKey, mapValue);
            }
        }
    }

    public static class TestPrediction_Reducer
            extends Reducer<PairWriteable, PairWriteable, PairWriteable, PairWriteable> {
        PairWriteable reduceValue = new PairWriteable();

        @Override
        protected void reduce(PairWriteable key, Iterable<PairWriteable> values,
                Context context)
                throws IOException, InterruptedException {
            boolean flag = false;
            String tempClass = null;
            double tempProbably = 0.0;
            for (PairWriteable value : values) {
                String className = value.getFirst();
                String probably = value.getSecond();
                if (flag != true) {
                    tempClass = className;
                    tempProbably = Double.parseDouble(probably);
                    flag = true;
                } else {
                    if (Double.parseDouble(probably) > tempProbably) {
                        tempClass = className;
                        tempProbably = Double.parseDouble(probably);
                    }
                }
            }
            reduceValue.set(tempClass, Double.toString(tempProbably));
            context.write(key, reduceValue);
        }
    }

    @Override
    public int run(String[] strings) throws Exception {
        Configuration conf = getConf();
        FileSystem hdfs = FileSystem.get(conf);

        Path predictionPath = new Path(Config.PREDICTION_PATH);
        if (hdfs.exists(predictionPath))
            hdfs.delete(predictionPath, true);

        Job job_Prediction = Job.getInstance(conf, "TestPrediction");
        job_Prediction.setJarByClass(TestPrediction.class);

        job_Prediction.setMapperClass(TestPrediction_Mapper.class);
        job_Prediction.setCombinerClass(TestPrediction_Reducer.class);
        job_Prediction.setReducerClass(TestPrediction_Reducer.class);

        job_Prediction.setMapOutputKeyClass(PairWriteable.class);
        job_Prediction.setMapOutputValueClass(PairWriteable.class);
        job_Prediction.setOutputKeyClass(PairWriteable.class);
        job_Prediction.setOutputValueClass(PairWriteable.class);

        FileInputFormat.setInputDirRecursive(job_Prediction, true);
        FileInputFormat.addInputPath(job_Prediction, new Path(Config.PREPARATION_PATH));
        FileOutputFormat.setOutputPath(job_Prediction, new Path(Config.PREDICTION_PATH));
        return job_Prediction.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(),
                new TestPrediction(), args);
        System.exit(res);
    }
}
