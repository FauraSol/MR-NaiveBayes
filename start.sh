# hdfs dfs -put /root/Naive_Bayes/data/* /zsq
hadoop jar /root/Naive_Bayes/Naive_Bayes.jar com.zsq.NB.Naive_Bayes > /root/Naive_Bayes/testing.log


#check
# hdfs dfs -cat /zsq/intermediate/doc_count/part-r-00000
# hdfs dfs -cat /zsq/intermediate/word_count/part-r-00000
# hdfs dfs -cat /zsq/result/preparation/part-r-00000
# hdfs dfs -cat /zsq/result/prediction/part-r-00000
