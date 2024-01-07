package com.zsq.NB;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ToolRunner;

public class NaiveBayes {
  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    DocCounter docCounter = new DocCounter();
    ToolRunner.run(conf, docCounter, args);
  }
}
