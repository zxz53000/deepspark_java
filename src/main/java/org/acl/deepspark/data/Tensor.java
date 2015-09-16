package org.acl.deepspark.data;

import org.jblas.DoubleMatrix;
import org.jblas.exceptions.SizeException;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by Jaehong on 2015-09-02.
 */
public class Tensor implements Serializable {

    protected int[] dimShape;         // dimShape = {kernels, channels, rows, cols}
    protected DoubleMatrix[] data;  // data = DoubleMatrix[kernels * channels]

    public enum init {
        ZEROS, ONES, UNIFORM, GAUSSIAN
    }

    protected Tensor() {
        dimShape = new int[] {1, 1, 1, 1};
    }

    protected Tensor(int... newDim) {
        this();
        if (newDim != null) {
            if (newDim.length > 4)
                throw new IllegalStateException(String.format("Only support (n <= 4) dimensional tensor, current: %d", newDim.length));
            /* dimShape = {kernels, channels, rows, cols} */
            System.arraycopy(newDim, 0, dimShape, 4-newDim.length, newDim.length);
            data = new DoubleMatrix[dimShape[0]*dimShape[1]];
        }
    }

    protected Tensor(Tensor.init init, int[] newDim) {
        this(newDim);
        int length = dimShape[0]*dimShape[1];
        for (int i = 0; i < length; i++) {
            switch (init) {
                case ZEROS:
                    data[i] = DoubleMatrix.zeros(dimShape[2], dimShape[3]);
                    break;

                case ONES:
                    data[i] = DoubleMatrix.ones(dimShape[2], dimShape[3]);
                    break;

                case UNIFORM:
                    data[i] = DoubleMatrix.rand(dimShape[2], dimShape[3]);
                    break;

                case GAUSSIAN:
                    data[i] = DoubleMatrix.randn(dimShape[2], dimShape[3]);
                    break;
            }
        }
    }

    protected Tensor(double[] newData, int[] newDim) {
        this(newDim);
        assertMatchSize(newData, newDim);

        int length = dimShape[0]*dimShape[1];
        int matSize = dimShape[2]*dimShape[3];
        for (int i = 0 ; i < length; i++) {
            double[] subArr = new double[matSize];
            System.arraycopy(newData, i*matSize, subArr, 0, subArr.length);
            data[i] = new DoubleMatrix(dimShape[2], dimShape[3], subArr);
        }
    }

    protected Tensor(DoubleMatrix[] newData, int[] newDim) {
        data = newData;
        dimShape = newDim;
    }

    public DoubleMatrix[] data() {
        return data;
    }

    public int[] shape() {
        return dimShape;
    }

    public int length() {
        int length = 1;
        for (int dim : dimShape)
            length *= dim;
        return length;
    }

    public DoubleMatrix slice(int kernelIdx) {
        return slice(kernelIdx, 0);
    }

    public DoubleMatrix slice(int kernelIdx, int channelIdx) {
        return data[kernelIdx*dimShape[1] + channelIdx];
    }

    public static Tensor create(double[] newData, int[] newDim) {
        return new Tensor(newData, newDim);
    }

    public static Tensor zeros(int... shape) {
        return new Tensor(init.ZEROS, shape);
    }

    public static Tensor ones(int... shape) {
        return new Tensor(init.ONES, shape);
    }

    public static Tensor rand(int... shape) {
        return new Tensor(init.UNIFORM, shape);
    }

    public static Tensor randn(int... shape) {
        return new Tensor(init.GAUSSIAN, shape);
    }

    public Tensor add(double d) {
        Tensor tensor = new Tensor(dimShape);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            tensor.data[i] = data[i].add(d);
        }
        return tensor;
    }

    public Tensor add(Tensor t) {
        assertSameLength(t);

        Tensor tensor = new Tensor(dimShape);
        int length = data.length;
        for (int i = 0; i < length; i++) {
            tensor.data[i] = data[i].add(t.data[i]);
        }
        return tensor;
    }

    public Tensor addi(double d) {
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            data[i].addi(d);
        }
        return this;
    }

    public Tensor addi(Tensor t) {
        assertSameLength(t);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            data[i].addi(t.data[i]);
        }
        return this;
    }

    public Tensor sub(double d) {
        Tensor tensor = new Tensor(dimShape);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            tensor.data[i] = data[i].sub(d);
        }
        return tensor;
    }

    public Tensor sub(Tensor t) {
        assertSameLength(t);

        Tensor tensor = new Tensor(dimShape);
        int length = data.length;
        for (int i = 0; i < length; i++) {
            tensor.data[i] = data[i].sub(t.data[i]);
        }
        return tensor;
    }

    public Tensor subi(double d) {
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            data[i].subi(d);
        }
        return this;
    }

    public Tensor subi(Tensor t) {
        assertSameLength(t);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            data[i].subi(t.data[i]);
        }
        return this;
    }

    public Tensor mul(double d) {
        Tensor tensor = new Tensor(dimShape);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            tensor.data[i] = data[i].mul(d);
        }
        return tensor;
    }

    public Tensor mul(Tensor t) {
        assertSameLength(t);

        Tensor tensor = new Tensor(dimShape);
        int length = data.length;
        for (int i = 0; i < length; i++) {
            tensor.data[i] = data[i].mul(t.data[i]);
        }
        return tensor;
    }

    public Tensor muli(double d) {
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            data[i].muli(d);
        }
        return this;
    }

    public Tensor muli(Tensor t) {
        assertSameLength(t);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            data[i].muli(t.data[i]);
        }
        return this;
    }

    public Tensor mmul(Tensor t) {
        assertMultipliesWith(t);
        Tensor tensor = new Tensor(dimShape[0], dimShape[1], dimShape[2], t.dimShape[3]);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            tensor.data[i] = data[i].mmul(t.data[i]);
        }
        return tensor;
    }

    public Tensor div(double d) {
        Tensor tensor = new Tensor(dimShape);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            tensor.data[i] = data[i].div(d);
        }
        return tensor;
    }

    public Tensor div(Tensor t) {
        assertSameLength(t);

        Tensor tensor = new Tensor(dimShape);
        int length = data.length;
        for (int i = 0; i < length; i++) {
            tensor.data[i] = data[i].div(t.data[i]);
        }
        return tensor;
    }

    public Tensor divi(double d) {
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            data[i].divi(d);
        }
        return this;
    }

    public Tensor divi(Tensor t) {
        assertSameLength(t);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            data[i].divi(t.data[i]);
        }
        return this;
    }

    public Tensor transpose() {
        Tensor t = new Tensor(dimShape[0], dimShape[1], dimShape[3], dimShape[2]);
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            t.data[i] = data[i].transpose();
        }
        return t;
    }

    public double sum() {
        double sum = 0;
        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            sum += data[i].sum();
        }
        return sum;
    }

    public Tensor dup() {
        Tensor tensor = new Tensor(dimShape.clone());
        int length = data.length;
        System.arraycopy(data, 0, tensor.data, 0, length);
        return tensor;
    }

    public static Tensor merge(Tensor... tensors) {
        // merged Tensors must have same lengths
        for (Tensor t : tensors) {
            tensors[0].assertSameLength(t);
        }
        int kernels = tensors[0].shape()[0];
        int channel = tensors[0].shape()[1];
        Tensor ret = new Tensor(kernels*tensors.length, channel,
                                tensors[0].shape()[2], tensors[0].shape()[3]);

        int dataSize = kernels*channel;
        for (int i = 0 ; i < tensors.length; i++) {
            System.arraycopy(tensors[i].data, 0, ret.data, i*dataSize, dataSize);
        }
        return ret;
    }

    public double[] toArray() {
        double[] arr = new double[length()];
        int matSize = dimShape[2]*dimShape[3];       // row x col

        int length = data.length;
        for (int i = 0 ; i < length; i++) {
            System.arraycopy(data[i].data, 0, arr, i*matSize, matSize);
        }
        return arr;
    }

    public Tensor reshape(int... shape) {
        return Tensor.create(toArray(), shape);
    }

    protected void assertSameLength(Tensor a) {
        if (!Arrays.equals(dimShape, a.shape())) {
            throw new SizeException(String.format("Tensors must have same length (is: {%d,%d,%d,%d} and {%d,%d,%d,%d})",
                                                    dimShape[0], dimShape[1], dimShape[2], dimShape[3],
                                                    a.dimShape[0], a.dimShape[1], a.dimShape[2], a.dimShape[3]));
        }
    }

    protected void assertMatchSize(double[] data, int[] shape) {
        int length = 1;
        for (int i = 0 ; i < shape.length; i++)
            length *= shape[i];

        if (data != null && data.length != length) {
            throw new SizeException(
                    "Passed data must match shape dimensions.");
        }
    }

    protected void assertMultipliesWith(Tensor t) {
        if (t.dimShape[0] != dimShape[0] || t.dimShape[1] != dimShape[1]) {
            throw new SizeException(String.format("Tensors must have same kernel and channel size (" +
                                    "is {%d,%d} and {%d,%d}", dimShape[0], dimShape[1], t.dimShape[0], t.dimShape[1]));
        } else {
            if (dimShape[3] != t.dimShape[2])
                throw new SizeException("Number of columns of left matrix must be equal to number of rows of right matrix.");
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0 ; i < dimShape[0]; i++) {
            builder.append(String.format("%d th kernels", i)).append("\n");
            for (int j = 0; j < dimShape[1]; j++) {
                builder.append(String.format("%d th channels", j)).append("\n");
                builder.append(data[i*dimShape[1] + j].toString()).append("\n");
            }
        }
        return builder.toString();
    }
}
