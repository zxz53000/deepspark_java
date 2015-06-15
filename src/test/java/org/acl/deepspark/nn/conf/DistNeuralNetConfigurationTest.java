package org.acl.deepspark.nn.conf;

import java.util.Date;

import org.acl.deepspark.data.Sample;
import org.acl.deepspark.nn.conf.spark.DistNeuralNetConfiguration;
import org.acl.deepspark.nn.layers.FullyConnLayer;
import org.acl.deepspark.nn.layers.cnn.ConvolutionLayer;
import org.acl.deepspark.nn.layers.cnn.PoolingLayer;
import org.acl.deepspark.utils.MnistLoader;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.jblas.DoubleMatrix;


public class DistNeuralNetConfigurationTest {
	
	public static final int nTest = 1000;
	public static final int minibatch = 1000;
	
	public static void main(String[] args) {
		SparkConf conf = new SparkConf().setAppName("DeepSpark CNN Test Driver");
		JavaSparkContext sc = new JavaSparkContext(conf);
		
		System.out.println("Data Loading...");
		
		Sample[] train_samples = MnistLoader.loadFromHDFS("mnist_data/train_data.txt",true);
		Sample[] test_samples = MnistLoader.loadFromHDFS("mnist_data/test_data.txt",true);
		
		// configure network
		DistNeuralNetConfiguration net = new DistNeuralNetConfiguration(0.1, 3, 1000, sc, true);
		net.addLayer(new ConvolutionLayer(9, 9, 20)); // conv with 20 filters (9x9)
		net.addLayer(new PoolingLayer(2)); // max pool
		net.addLayer(new FullyConnLayer(200)); // hidden
		net.addLayer(new FullyConnLayer(10)); // output
		
		System.out.println("Start Learning...");
		Date startTime = new Date();
		net.training(train_samples);
		DoubleMatrix[] matrix;
		
		System.out.println(String.format("Testing... with %d samples...", nTest));
		int count = 0;
		for(int j = 0 ; j < nTest; j++) {
			matrix = test_samples[j].data;
			if(test_samples[j].label.argmax() == net.getOutput(matrix)[0].argmax())
				count++;
		}
		
		System.out.println(String.format("Accuracy: %f %%", (double) count / nTest * 100));
		Date endTime = new Date();
		
		long time = endTime.getTime() - startTime.getTime();
		
		System.out.println(String.format("Training time: %f secs", (double) time / 1000));
	} 
}