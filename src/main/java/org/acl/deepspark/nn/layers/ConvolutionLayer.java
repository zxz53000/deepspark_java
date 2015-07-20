package org.acl.deepspark.nn.layers;

import java.io.Serializable;

import org.acl.deepspark.data.Weight;
import org.acl.deepspark.nn.conf.LayerConf;
import org.acl.deepspark.nn.functions.ActivatorType;
import org.acl.deepspark.nn.layers.BaseLayer;
import org.acl.deepspark.nn.weights.WeightUtil;
import org.acl.deepspark.utils.MathUtils;
import org.jblas.DoubleMatrix;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.convolution.Convolution;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.NDArrayUtil;

public class ConvolutionLayer implements Serializable, Layer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 140807767171115076L;
	private int filterRows, filterCols, numFilters; // filter spec.
	private DoubleMatrix[][] W; // filterId, x, y
	private double[] bias;
	
	// momentum 
	private double momentumFactor = 0.0;
	private DoubleMatrix[][] prevDeltaW;
	private double[] prevDeltaBias;
		
	// weight decay
	private double decayLambda = 0.00001;
	
	private int[] stride = {1, 1};
	private int zeroPadding = 0;
	private boolean useZeroPadding = true;

	public ConvolutionLayer() {

	}

	public ConvolutionLayer(ActivatorType activator) {

	}

	public ConvolutionLayer(int filterRows, int filterCols, int numFilters) {
		super();
		this.filterRows = filterRows;
		this.filterCols = filterCols;
		this.numFilters = numFilters;
	}
	
	public ConvolutionLayer(int filterRows, int filterCols, int numFilters, double momentum, double decayLambda) {
		this(filterRows, filterCols, numFilters);
		this.momentumFactor = momentum;
		this.decayLambda = decayLambda;
	}
	
	public ConvolutionLayer(DoubleMatrix input, int filterRows, int filterCols, int numFilters) {
		super(input);
		this.filterRows = filterRows;
		this.filterCols = filterCols;
		this.numFilters = numFilters;
		initWeights();
	}
	
	public ConvolutionLayer(DoubleMatrix[] input, int filterRows, int filterCols, int numFilters) {
		super(input);
		this.filterRows = filterRows;
		this.filterCols = filterCols;
		this.numFilters = numFilters;
		initWeights();
	}
	
	public ConvolutionLayer(DoubleMatrix input, int filterRows, int filterCols, int numFilters, double momentum) {
		this(input, filterRows, filterCols, numFilters);
		momentumFactor = momentum;
	}
	
	public ConvolutionLayer(DoubleMatrix[] input, int filterRows, int filterCols, int numFilters,double momentum) {
		this(input, filterRows, filterCols, numFilters);
		momentumFactor = momentum;
	}
	
	public void setFilterWeights(DoubleMatrix[][] filters) {
		W = filters;
	}
	
	public DoubleMatrix[][] getFilterWeights() {
		return W;
	}
	
	@Override
	public void initWeights() {
		if (W == null || bias == null) {
			W = new DoubleMatrix[numFilters][numChannels];
			prevDeltaW = new DoubleMatrix[numFilters][numChannels];
			
			bias = new double[numFilters];
			prevDeltaBias = new double[numFilters];
			
			for(int i = 0; i < numFilters; i++) {
				for(int j = 0; j < numChannels; j++) {
					W[i][j] = WeightUtil.randInitWeights(filterRows, filterCols, dimIn);
					prevDeltaW[i][j] = DoubleMatrix.zeros(filterRows, filterCols);
				}
				bias[i] = 0.0;
				prevDeltaBias[i] = 0;
			}
		}
	}

	public int getNumOfChannels() {
		return numChannels;
	}
	
	public int getNumOfFilter() {
		return numFilters;
	}
	
	private int getOutputRows() {
		return  dimRows - filterRows + 1;
	}
	
	private int getOutputCols() {
		return  dimCols - filterCols + 1;
	}
	
	// Convolution of multiple channel input images
	public DoubleMatrix[] convolution() {
		DoubleMatrix[] data = new DoubleMatrix[numFilters];
		// TODO: check dims(image) > dims(filter)
		for(int i = 0; i < numFilters; i++) {
			data[i] = DoubleMatrix.zeros(getOutputRows(), getOutputCols());
			for(int j = 0; j < numChannels; j++) {
				data[i].addi(MathUtils.convolution(input[j], W[i][j], MathUtils.VALID_CONV));
			}
			data[i].addi(bias[i]);
		}
		return data;
	}
	
	@Override
	public DoubleMatrix[] getOutput() {
		output = activate(convolution());
		return output;
	}
	
	@Override
	public void setDelta(DoubleMatrix[] propDelta) {
		delta = propDelta;
		for(int i = 0 ; i < delta.length; i++)
			delta[i].muli(output[i].mul(output[i].mul(-1.0).add(1.0)));
	}
	
	// TODO:
	@Override
	public DoubleMatrix[][] deriveGradientW() {
		DoubleMatrix[][] gradient = new DoubleMatrix[numFilters][numChannels];
		
		// update Weights
		for (int i = 0; i < numFilters; i++)
			for (int j = 0; j < numChannels; j++)
				gradient[i][j] = MathUtils.convolution(input[j], getDelta()[i], MathUtils.VALID_CONV);		
		return gradient;
	}

	@Override
	public void update(DoubleMatrix[][] gradW, double[] gradB) {
		for (int i = 0; i < numFilters; i++) {
			for (int j = 0; j < numChannels; j++) {				
				prevDeltaW[i][j].muli(momentumFactor);
				prevDeltaW[i][j].addi(W[i][j].mul(learningRate * decayLambda));
				prevDeltaW[i][j].addi(gradW[i][j].muli(learningRate));
				
				//prevDeltaBias[i] *= momentumFactor;
				prevDeltaBias[i] = (gradB[i]  + bias[i] * decayLambda)* learningRate;
				W[i][j].subi(prevDeltaW[i][j]);
				bias[i] -= prevDeltaBias[i];
			}
		}
	}
	
	@Override
	public DoubleMatrix[] deriveDelta() {
		if (delta == null || delta.length <= 0)
			return null;
		
		DoubleMatrix[] propDelta = new DoubleMatrix[numChannels];
		DoubleMatrix filter;
		
		// TODO: check dims(image) > dims(filter)
		for (int j = 0; j < numChannels; j++) {
			propDelta[j] = DoubleMatrix.zeros(dimRows, dimCols);
			for (int i = 0; i < numFilters; i++) {
				filter = new DoubleMatrix(W[i][j].toArray2());
				
				MathUtils.flip(filter);
				propDelta[j].addi(MathUtils.convolution(delta[i], filter, MathUtils.FULL_CONV));
			}
		}
		return propDelta;
	}

	@Override
	public void applyDropOut() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int[] initWeights(int[] dim) {
		int[] outDim = new int[3];
		
		this.dimRows = dim[0];
		this.dimCols = dim[1];
		this.numChannels = dim[2];
		this.dimIn = dimRows * dimCols * numChannels;
		initWeights();
		
		outDim[0] = dim[0] - filterRows +1; //output row dimension
		outDim[1] = dim[1] - filterCols +1; //output col dimension
		outDim[2] = numFilters; //output channel dimension
		
		return outDim;
	}

	@Override
	public int[] getWeightInfo() {
		int[] info = {numFilters, numChannels, filterRows, filterCols};
		return info;
	}

	@Override
	public Weight createWeight(LayerConf conf, int[] input) {
		return null;
	}

	@Override
	public INDArray generateOutput(Weight weight, INDArray input) {
		int[] dim = new int[3];
		int[] inputDim = input.shape(); // 0: x, 1: y, 2: # of channel;
		int[] kernelDim = weight.getShape(); // 0: x, 1: y, 2: # of channel, 3: # of filter;
		dim[0] = inputDim[0] - kernelDim[0] + 1;
		dim[1] = inputDim[1] - kernelDim[1] + 1;
		dim[2] = kernelDim[3];
		
		INDArray output = Nd4j.zeros(dim);
		
		// TODO: check dims(image) > dims(filter)
		for(int i = 0; i < numFilters; i++) {
			for(int j = 0; j < numChannels; j++) {
				output.sl.addi(MathUtils.convolution(input[j], W[i][j], MathUtils.VALID_CONV));
			}
			data[i].addi(bias[i]);
		}
		return output;
	}

	@Override
	public INDArray deriveDelta(Weight weight, INDArray error, INDArray output) {
		return null;
	}

	@Override
	public INDArray gradient(INDArray input, INDArray error) {
		return null;
	}

	@Override
	public INDArray activate(INDArray output) {
		// TODO Auto-generated method stub
		return null;
	}
}
