package org.deeplearning4j.nn.params;

import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.api.ParamInitializer;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.Layer;
import org.deeplearning4j.nn.conf.layers.samediff.BaseSameDiffLayer;
import org.deeplearning4j.nn.weights.WeightInitUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.util.ArrayUtil;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.nd4j.linalg.indexing.NDArrayIndex.interval;
import static org.nd4j.linalg.indexing.NDArrayIndex.point;

@Slf4j
public class SameDiffParamInitializer implements ParamInitializer {

    private static final SameDiffParamInitializer INSTANCE = new SameDiffParamInitializer();

    public static SameDiffParamInitializer getInstance() {
        return INSTANCE;
    }

    @Override
    public int numParams(NeuralNetConfiguration conf) {
        return numParams(conf.getLayer());
    }

    @Override
    public int numParams(Layer layer) {
        BaseSameDiffLayer sd = (BaseSameDiffLayer)layer;
        Map<String,int[]> m = sd.paramShapes();
        int n = 0;
        for(int[] arr : m.values()){
            n += ArrayUtil.prod(arr);
        }
        return n;
    }

    @Override
    public List<String> paramKeys(Layer layer) {
        BaseSameDiffLayer sd = (BaseSameDiffLayer)layer;
        return sd.paramKeys();
    }

    @Override
    public List<String> weightKeys(Layer layer) {
        BaseSameDiffLayer sd = (BaseSameDiffLayer)layer;
        return sd.weightKeys();
    }

    @Override
    public List<String> biasKeys(Layer layer) {
        BaseSameDiffLayer sd = (BaseSameDiffLayer)layer;
        return sd.biasKeys();
    }

    @Override
    public boolean isWeightParam(Layer layer, String key) {
        return weightKeys(layer).contains(key);
    }

    @Override
    public boolean isBiasParam(Layer layer, String key) {
        return biasKeys(layer).contains(key);
    }

    @Override
    public Map<String, INDArray> init(NeuralNetConfiguration conf, INDArray paramsView, boolean initializeParams) {
        BaseSameDiffLayer sd = (BaseSameDiffLayer) conf.getLayer();
        Map<String,INDArray> out = subsetAndReshape(sd.paramKeys(), sd.paramShapes(), paramsView);
        if(initializeParams){
            //TODO
            log.warn("***** SameDiffParamInitializer: Parameter initialization not yet implemented *****");
        }
        return out;
    }

    @Override
    public Map<String, INDArray> getGradientsFromFlattened(NeuralNetConfiguration conf, INDArray gradientView) {
        BaseSameDiffLayer sd = (BaseSameDiffLayer) conf.getLayer();
        return subsetAndReshape(sd.paramKeys(), sd.paramShapes(), gradientView);
    }

    private Map<String,INDArray> subsetAndReshape(List<String> params, Map<String,int[]> paramShapes, INDArray view){
        Map<String,INDArray> out = new LinkedHashMap<>();
        int soFar = 0;
        for(String s : params){
            int[] sh = paramShapes.get(s);
            int length = ArrayUtil.prod(sh);
            INDArray sub = view.get(point(0), interval(soFar, soFar + length));
            if(!Arrays.equals(sub.shape(), sh)){
                sub = sub.reshape(WeightInitUtil.DEFAULT_WEIGHT_INIT_ORDER, sh); //TODO do we want to allow users to override initialization order?
            }
            out.put(s, sub);

            soFar += length;
        }
        return out;
    }
}
