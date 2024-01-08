package com.zsq.NB;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;

public class NaiveBayes {
  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    DocCounter docCounter = new DocCounter();
    ToolRunner.run(conf, docCounter, args);
    WordCounter wordCounter = new WordCounter();
    ToolRunner.run(conf, wordCounter, args);
    TestPreparation testPreparation = new TestPreparation();
    ToolRunner.run(conf, testPreparation, args);
    // 预测测试集文件类别
    TestPrediction testPrediction = new TestPrediction();
    ToolRunner.run(conf, testPrediction, args);
    // 评估测试效果，计算precision，recall，F1
    Statistics stat = new Statistics();
    ToolRunner.run(conf, stat, args);
  }
}
