package com.netcloudai.bigdata.metrics;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * Desc 演示Flink-Metrics监控
 * 在Map算子中提供一个Counter,统计map处理的数据条数,运行之后再WebUI上进行监控
 */
public class Demo01_MetricsCounter {
    public static void main(String[] args) throws Exception {
        //TODO 0.env
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setRuntimeMode(RuntimeExecutionMode.AUTOMATIC);

        //TODO 1.source
        DataStream<String> lines = env.socketTextStream("bigdata01", 7777);


        //TODO 2.transformation
        SingleOutputStreamOperator<String> words = lines.flatMap(new FlatMapFunction<String, String>() {
            @Override
            public void flatMap(String value, Collector<String> out) throws Exception {
                String[] arr = value.split(" ");
                for (String word : arr) {
                    out.collect(word);
                }
            }
        });

        SingleOutputStreamOperator<Tuple2<String, Integer>> wordAndOne = words
                .map(new RichMapFunction<String, Tuple2<String, Integer>>() {
                    Counter myCounter;//用来记录map处理了多少个单词
                    //对Counter进行初始化
                    @Override
                    public void open(Configuration parameters) throws Exception {
                        myCounter = getRuntimeContext().getMetricGroup().addGroup("myGroup").counter("myCounter");
                    }
                    //处理单词,将单词记为(单词,1)
                    @Override
                    public Tuple2<String, Integer> map(String value) throws Exception {
                        myCounter.inc();//计数器+1
                        return Tuple2.of(value, 1);
                    }
                });

        SingleOutputStreamOperator<Tuple2<String, Integer>> result = wordAndOne.keyBy(t -> t.f0).sum(1);

        //TODO 3.sink
        result.print();

        //TODO 4.execute
        env.execute();
    }
}
// 应用程序打包上传执行
// 查看WebUI
