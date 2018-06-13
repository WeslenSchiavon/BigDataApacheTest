/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ufpel.storm;

import com.ufpel.util.Monitorador;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.storm.Config;
import org.apache.storm.Constants;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

//There are a variety of bolt types. In this case, use BaseBasicBolt
public class WordCount extends BaseBasicBolt {
    //Create logger for this class

    private static final Logger logger = LogManager.getLogger(WordCount.class);
    //For holding words and counts
    Map<String, Integer> counts = new HashMap<String, Integer>();
    //How often to emit a count of words
    private Integer emitFrequency;

    // Default constructor
    public WordCount() {
        emitFrequency = 5; // Default to 60 seconds
    }

    // Constructor that sets emit frequency
    public WordCount(Integer frequency) {
        emitFrequency = frequency;
    }

    //Configure frequency of tick tuples for this bolt
    //This delivers a 'tick' tuple on a specific interval,
    //which is used to trigger certain actions
    @Override
    public Map<String, Object> getComponentConfiguration() {
        Config conf = new Config();
        conf.put(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS, emitFrequency);
        return conf;
    }

    //execute is called to process tuples
    @Override
    public void execute(Tuple tuple, BasicOutputCollector collector) {
        //If it's a tick tuple, emit all words and counts
        if (tuple.getSourceComponent().equals(Constants.SYSTEM_COMPONENT_ID)
                && tuple.getSourceStreamId().equals(Constants.SYSTEM_TICK_STREAM_ID)) {
            for (String word : counts.keySet()) {
                Integer count = counts.get(word);
                collector.emit(new Values(word, count));
                //logger.info("Emitting a count of " + count + " for word " + word);
            }

            synchronized (this) {
                try {
                    PrintStream ps = new PrintStream(new File("Relatorio.txt"));
                    logger.info("# tarefa " + Thread.currentThread().getId() + " terminada!");
                    ps.println("Tempo: " + Monitorador.getTimeExecutation() + "s");
                    ps.println("Memoria: " + Monitorador.getMaxMemoryUsage() + "MB");

                    this.wait();
                } catch (InterruptedException ex) {
                    logger.error("erro:" + ex);
                } catch (FileNotFoundException ex) {
                    java.util.logging.Logger.getLogger(WordCount.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } else {
            //Get the word contents from the tuple
            String word = tuple.getString(0);
            //Have we counted any already?
            Integer count = counts.get(word);
            if (count == null) {
                count = 0;
            }
            //Increment the count and store it
            count++;
            counts.put(word, count);
        }
    }

    //Declare that this emits a tuple containing two fields; word and count
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("word", "count"));
    }
}
