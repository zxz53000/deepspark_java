
package org.acl.deepspark.nn.weights;

import java.io.Serializable;

import org.jblas.DoubleMatrix;


public class WeightUtil implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1196601489934475280L;

	public static DoubleMatrix randInitWeights(int dimRow, int dimCol) {
		return DoubleMatrix.randn(dimRow, dimCol).divi(10);
	}
	
	public static DoubleMatrix accum(DoubleMatrix[] matrices) {
		DoubleMatrix result = new DoubleMatrix(matrices[0].rows, matrices[0].columns);
		for(DoubleMatrix matrix : matrices) {
			result.add(matrix);
		}
		return result;
	}
	
	public static DoubleMatrix flat2Vec(DoubleMatrix matrix) {
		if (matrix != null)
			return new DoubleMatrix(matrix.toArray());
		return null;
	}
	
	public static DoubleMatrix flat2Vec(DoubleMatrix[] matrices) {
		if (matrices != null) {
			int length = matrices.length;
			int dim = matrices[0].length;
			
			double[] data = new double[length * dim];
			for(int i = 0 ; i < length; i++) {
				for (int j = 0; j < dim; j++) {
					data[i * dim + j] = matrices[i].get(j);
				}
			}
			return new DoubleMatrix(data);
		}
		return null;
	}
}
