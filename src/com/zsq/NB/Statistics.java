package com.zsq.NB;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;

public class Statistics extends Configured implements Tool {
    public static void GetEvaluation(Configuration conf) throws IOException {
        // 读取TextPrediction输出的文档
        String classFilePath = Config.PREDICTION_PATH + "/part-r-00000";
        FileSystem fs = FileSystem.get(URI.create(classFilePath), conf);
        FSDataInputStream fsr = fs.open(new Path(classFilePath));
        ArrayList<String> ClassNames = new ArrayList<>();
        ArrayList<Integer> TruePositive = new ArrayList<>();
        ArrayList<Integer> FalseNegative = new ArrayList<>();
        ArrayList<Integer> FalsePositive = new ArrayList<>();
        ArrayList<Double> precision = new ArrayList<>();
        ArrayList<Double> recall = new ArrayList<>();
        ArrayList<Double> F1 = new ArrayList<>();
        BufferedReader reader = null;
        Integer temp = 0;
        try {
            reader = new BufferedReader(new InputStreamReader(fsr));
            String lineValue = null;
            while ((lineValue = reader.readLine()) != null) {

                String[] values = lineValue.split("\\s");
                if (!ClassNames.contains(values[0])) {
                    ClassNames.add(values[0]);
                    TruePositive.add(0);
                    FalseNegative.add(0);
                    FalsePositive.add(0);
                }
                if (!ClassNames.contains(values[2])) {
                    ClassNames.add(values[2]);
                    TruePositive.add(0);
                    FalseNegative.add(0);
                    FalsePositive.add(0);
                }
                if (values[0].equals(values[2])) {
                    temp = TruePositive.get(ClassNames.indexOf(values[2])) + 1;
                    TruePositive.set(ClassNames.indexOf(values[2]), temp);
                } else {
                    temp = FalseNegative.get(ClassNames.indexOf(values[0])) + 1;
                    FalseNegative.set(ClassNames.indexOf(values[0]), temp);
                    temp = FalsePositive.get(ClassNames.indexOf(values[2])) + 1;
                    FalsePositive.set(ClassNames.indexOf(values[2]), temp);
                }
            }
            for (int i = 0; i < ClassNames.size(); i++) {
                int TP = TruePositive.get(i);
                int FP = FalsePositive.get(i);
                int FN = FalseNegative.get(i);
                double p = TP * 1.0 / (TP + FP);
                double r = TP * 1.0 / (TP + FN);
                double F = 2 * p * r / (p + r);
                precision.add(p);
                recall.add(r);
                F1.add(F);
            }

            double p_Sum_Ma = 0.0;
            double r_Sum_Ma = 0.0;
            double F1_Sum_Ma = 0.0;
            Integer TP_Sum_Mi = 0;
            Integer FN_Sum_Mi = 0;
            Integer FP_Sum_Mi = 0;
            int n = ClassNames.size();
            for (int i = 0; i < n; i++) {
                p_Sum_Ma += precision.get(i);
                r_Sum_Ma += recall.get(i);
                F1_Sum_Ma += F1.get(i);
                TP_Sum_Mi += TruePositive.get(i);
                FN_Sum_Mi += FalseNegative.get(i);
                FP_Sum_Mi += FalsePositive.get(i);
            }

            double p_Ma = p_Sum_Ma / n;
            double r_Ma = r_Sum_Ma / n;
            double F1_Ma = F1_Sum_Ma / n;

            double p_Mi = TP_Sum_Mi * 1.0 / (TP_Sum_Mi + FP_Sum_Mi);
            ;
            double r_Mi = TP_Sum_Mi * 1.0 / (TP_Sum_Mi + FN_Sum_Mi);
            double F1_Mi = 2 * p_Mi * r_Mi / (p_Mi + r_Mi);
            for (int i = 0; i < n; i++) {
                System.out.println(ClassNames.get(i) + "\tprecision: " + precision.get(i).toString());
                System.out.println(ClassNames.get(i) + "\trecall: " + recall.get(i).toString());
                System.out.println(ClassNames.get(i) + "\tF1: " + F1.get(i).toString());
            }
            System.out.println("Macroaveraged precision: " + p_Ma);
            System.out.println("Macroaveraged recall: " + r_Ma);
            System.out.println("Macroaveraged F1: " + F1_Ma);
            System.out.println("Microaveraged precision: " + p_Mi);
            System.out.println("Microaveraged recall: " + r_Mi);
            System.out.println("Microaveraged F1: " + F1_Mi);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            reader.close();
        }
    }

    @Override
    public int run(String[] strings) throws Exception {
        Configuration conf = getConf();
        GetEvaluation(conf);
        return 0;
    }

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(),
                new Statistics(), args);
        System.exit(res);
    }
}
