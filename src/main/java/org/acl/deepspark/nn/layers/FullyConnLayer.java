package org.acl.deepspark.nn.layers;

import org.acl.deepspark.nn.weights.WeightUtil;
import org.jblas.DoubleMatrix;

// Fully Connected HiddenLayer
public class FullyConnLayer extends BaseLayer {
	private DoubleMatrix W;
	private DoubleMatrix output;
	private double bias;

	/** Modified **/
	public FullyConnLayer(int nOut) {
		this.dimOut = nOut;
	}
	
	
	public FullyConnLayer(DoubleMatrix input, int nOut) {
		super(input, nOut);
		initWeights();
	}
	
	public FullyConnLayer(DoubleMatrix[] input, int nOut) {
		super(input, nOut);
		initWeights();
	}
	
	public void setWeight(DoubleMatrix weight) {
		this.W = weight;
	}
	
	public DoubleMatrix getWeight() {
		return W;
	}
	
	// output: dimOut x 1 column Vector
	@Override
	public DoubleMatrix[] getOutput() {
		DoubleMatrix[] preActivation = new DoubleMatrix[1];
		
		/** Modified **/
		if(W == null)
			initWeights();
		
		
		preActivation[0] = W.mmul(WeightUtil.flat2Vec(input));
		preActivation[0].add(bias);
		output = activate(preActivation[0]);
		return preActivation;
	}
	
	@Override
	public DoubleMatrix[] deriveDelta(DoubleMatrix[] delta) {
		double[] input = delta[0].transpose().mmul(W).toArray();
		DoubleMatrix[] propDelta = new DoubleMatrix[numChannels];
		
		int index = 0;
		for (int i = 0; i < numChannels; i++) {
			propDelta[i] = new DoubleMatrix(dimRows, dimCols);
			for (int n = 0; n < dimCols; n++) {
				for (int m = 0; m < dimRows; m++) {
					propDelta[i].put(m, n, input[index++]);
				}
			}
		}
		return propDelta;
	}

	@Override
	public DoubleMatrix[] update(DoubleMatrix[] propDelta) {
		DoubleMatrix delta = propDelta[0].mul(output.mul(output.mul(-1.0).add(1.0)));
		DoubleMatrix deltaW = delta.mmul(WeightUtil.flat2Vec(input).transpose());
		
		// weight update
		W.subi(deltaW.mul(learningRate));
		
		// propagate delta
		DoubleMatrix[] deltas = {delta};
		return deriveDelta(deltas);
	}

	@Override
	public void initWeights() {
		W = WeightUtil.randInitWeights(dimOut, dimIn);
		bias = 0.1;
	}

	@Override
	public void applyDropOut() {
		// TODO Auto-generated method stub
	}

	

	
}
