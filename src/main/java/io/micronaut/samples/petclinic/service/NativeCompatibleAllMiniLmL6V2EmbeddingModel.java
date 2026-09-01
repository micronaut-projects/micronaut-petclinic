package io.micronaut.samples.petclinic.service;

import ai.djl.huggingface.tokenizers.jni.LibUtils;
import ai.djl.huggingface.tokenizers.jni.TokenizersLibrary;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Runs the bundled all-MiniLM-L6-v2 model without constructing DJL's
 * {@code HuggingFaceTokenizer} facade.
 *
 * <p>The facade materializes character spans for every token. That callback
 * uses JNI to look up a Java constructor from the Rust tokenizer library,
 * which is not compatible with the native test image on all GraalVM versions.
 * The ONNX model only needs token ids, attention masks, and type ids, so this
 * adapter calls the lower-level tokenizer API and deliberately does not ask
 * for character spans.</p>
 */
final class NativeCompatibleAllMiniLmL6V2EmbeddingModel extends DimensionAwareEmbeddingModel {

    private static final String MODEL_RESOURCE = "/all-minilm-l6-v2.onnx";
    private static final String TOKENIZER_RESOURCE = "/all-minilm-l6-v2-tokenizer.json";
    private static final int DIMENSION = 384;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final Set<String> expectedInputs;
    private final TokenizersLibrary tokenizers;
    private final long tokenizerHandle;

    NativeCompatibleAllMiniLmL6V2EmbeddingModel() {
        try {
            environment = OrtEnvironment.getEnvironment();
            session = environment.createSession(readResource(MODEL_RESOURCE));
            expectedInputs = session.getInputNames();
            LibUtils.checkStatus();
            tokenizers = TokenizersLibrary.LIB;
            tokenizerHandle = tokenizers.createTokenizerFromString(readText(TOKENIZER_RESOURCE));
        } catch (IOException | OrtException e) {
            throw new IllegalStateException("Could not initialize the all-MiniLM-L6-v2 model", e);
        }
    }

    @Override
    public synchronized Response<Embedding> embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text to embed cannot be blank");
        }

        long encodingHandle = tokenizers.encode(tokenizerHandle, text, true);
        try {
            long[] ids = tokenizers.getTokenIds(encodingHandle);
            long[] attentionMask = tokenizers.getAttentionMask(encodingHandle);
            long[] typeIds = tokenizers.getTypeIds(encodingHandle);
            long[] shape = {1, ids.length};

            Map<String, OnnxTensor> inputs = new HashMap<>();
            OnnxTensor inputIds = OnnxTensor.createTensor(environment, LongBuffer.wrap(ids), shape);
            OnnxTensor inputAttentionMask = OnnxTensor.createTensor(
                    environment,
                    LongBuffer.wrap(attentionMask),
                    shape
            );
            inputs.put("input_ids", inputIds);
            inputs.put("attention_mask", inputAttentionMask);

            OnnxTensor inputTypeIds = null;
            if (expectedInputs.contains("token_type_ids")) {
                inputTypeIds = OnnxTensor.createTensor(environment, LongBuffer.wrap(typeIds), shape);
                inputs.put("token_type_ids", inputTypeIds);
            }

            try (OrtSession.Result result = session.run(inputs)) {
                float[][] tokenEmbeddings = ((float[][][]) result.get(0).getValue())[0];
                return Response.from(new Embedding(normalize(meanPool(tokenEmbeddings))));
            } finally {
                close(inputTypeIds);
                close(inputAttentionMask);
                close(inputIds);
            }
        } catch (OrtException e) {
            throw new IllegalStateException("Could not create an embedding", e);
        } finally {
            tokenizers.deleteEncoding(encodingHandle);
        }
    }

    @Override
    protected Integer knownDimension() {
        return DIMENSION;
    }

    private static float[] meanPool(float[][] tokenEmbeddings) {
        int tokenCount = tokenEmbeddings.length;
        float[] pooled = new float[DIMENSION];
        for (float[] tokenEmbedding : tokenEmbeddings) {
            for (int i = 0; i < DIMENSION; i++) {
                pooled[i] += tokenEmbedding[i];
            }
        }
        for (int i = 0; i < DIMENSION; i++) {
            pooled[i] /= tokenCount;
        }
        return pooled;
    }

    private static float[] normalize(float[] vector) {
        float length = 0.0f;
        for (float value : vector) {
            length += value * value;
        }
        float norm = (float) Math.sqrt(length);
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = vector[i] / norm;
        }
        return normalized;
    }

    private static byte[] readResource(String resource) throws IOException {
        try (InputStream input = NativeCompatibleAllMiniLmL6V2EmbeddingModel.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing classpath resource: " + resource);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private static String readText(String resource) throws IOException {
        return new String(readResource(resource), StandardCharsets.UTF_8);
    }

    private static void close(OnnxTensor tensor) {
        if (tensor != null) {
            tensor.close();
        }
    }
}
